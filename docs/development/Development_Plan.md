# Team Work 系統詳細開發計畫 (Development Plan)

這份文件從宏觀的 `RoadMap.md` 與 `Feature_Requirements.md` 中拆解出具體的開發步驟與執行順序。目前僅列出計畫，作為後續執行 (Execution) 的施工藍圖。

---

## 階段一：核心基礎建設與資料庫層 (Phase 1 - Foundation)
本階段目標：建立系統的地基、資料庫 Schema、以及最核心的非同步任務派發與狀態追蹤機制。

### 1.1 專案初始化與地基建設
1.  **建立 Spring Boot 專案**：設定 Java 21, Spring Boot 4.0.3, Gradle/Maven, 並匯入 Spring Web, Spring Data JPA, PostgreSQL Driver, Redis 等基礎依賴。
2.  **配置 Virtual Threads**：在 `application.yml` 中啟用 Java 21 虛擬執行緒 (`spring.threads.virtual.enabled=true`) 以支援高併發 Agent 執行。
3.  **配置 PostgreSQL 連線**：設定 JPA 連線字串、HikariCP 連線池。
4.  **配置 Redis 連線**：設定 RedisTemplate 與基本連線屬性。
5.  **建立全域異常處理 (Global Exception Handler)**：實作 `@RestControllerAdvice` 攔截 API 錯誤並回傳統一樣式。

### 1.2 資料庫 Schema 設計與實作 (Registry & State)
1.  **設計 `ai_models` 表**：(Model ID, Model Name, Provider, API Key [加密存放], Endpoint URL, Status)。建立對應 Entity 與 Repository。
2.  **設計 `tools` 表**：(Tool ID, Tool Name, Type [Built-in/MCP/Skill], Configuration JSON, Description)。建立 Entity 與 Repository。
3.  **設計 `agent_profiles` 表**：(Profile ID, Name, System Prompt, Default Model ID)。建立 Entity 與 Repository。
4.  **設計 `account_tool_permissions` 表 (RBAC)**：(Tenant ID / User ID, Tool ID)。建立多對多映射的 Entity。
5.  **設計 `tasks` 表 (Task State)**：(Task ID, Parent Task ID, Profile ID, Status [Pending/Running/Completed/Failed], Input Payload, Created At, Updated At)。建立 Entity 與 Repository。
6.  **設計 `task_outputs` 表**：(Output ID, Task ID, Output Type [Markdown/Code/JSON], Content URL / JSON Blob)。建立 Entity 與 Repository。
7.  **產生資料庫遷移腳本 (Flyway / Liquibase)** (可選但建議)：將上述 Schema 寫入版本控制。

### 1.3 核心任務派發器 (Task Dispatcher & Gateway)
1.  **定義 Task Request API**：建立 `TaskController`，接收前端包含 `profile_id`, `user_id`, `input_text` 的 POST 請求。
2.  **實作 Gateway 攔截與驗證邏輯**：
    *   驗證 User 是否存在。
    *   根據 `profile_id` 撈取 Agent 設定 (`agent_profiles`)。
    *   驗證 User 在 `account_tool_permissions` 中擁有的 Tools。
3.  **建立 Task 紀錄並非同步派發**：
    *   生成唯一的 `Task ID`。
    *   將狀態寫入 `tasks` 表 (`Pending`)。
    *   使用 `@Async` 呼叫 Agent Service 處理該 Task。
    *   API Controller 立即回傳 HTTP 200 與 `Task ID` 給前端。

---

## 階段二：Spring AI 代理人引擎與工具整合 (Phase 2 - Agent Engine)
本階段目標：讓 Agent 能夠基於上一階段讀取的配置，動態組裝 LLM、載入工具，並完整跑完 ReAct 迴圈。

### 2.1 Agent 動態組裝 (Dynamic Assembly)
1.  **實作 Model Factory**：根據傳入的 `ai_models` 設定，動態建立對應的 `ChatModel` 實例 (支援 OpenAI, Anthropic 等)。
2.  **實作 Tools Loader**：將傳入的 `tools` 資料，轉換為 Spring AI 可識別的 `FunctionCallback` 或標準化 Tool Bean。
3.  **實作 Context Builder**：將 User Input、長期記憶 (如果有) 與 System Prompt 組合。
4.  **組裝 Agent 實體**：將上述 ChatModel, Tools, Context 綁定至單一執行個體準備 Inference。

### 2.2 ReAct 迴圈與狀態回報 (Event-Driven ReAct)
1.  **實作 Agent Runner Service**：在 `@Async` 方法中執行 Spring AI 的呼叫迴圈。
2.  **實作 Chat Memory 與 Redis 整合**：每次迴圈呼叫時，從 Redis 讀取並更新對話歷史，確保 Token 不超限。
3.  **狀態更新機制**：開始呼叫前，將 `tasks` 狀態改為 `Running`；發生 Exception 則更新為 `Failed`。
4.  **Internal Event / Event Publisher 實作**：Agent 迴圈結束產生最終結果後，觸發 Spring ApplicationEvent (`TaskCompletedEvent`) 帶上最終產出資料。

### 2.3 任務產出物儲存 (Task Output Processing)
1.  **定義結構化 Response**：確保 LLM 輸出的最後結果符合自訂 JSON Schema (包含 Summary, Content, Artifacts)。
2.  **實作 Output Storage Service**：監聽 `TaskCompletedEvent`，解析產出物。
3.  **寫入資料庫或雲端**：將結構化 JSON 存入 `task_outputs` 表。若包含大型二進位檔，則另外實作 S3/MinIO 上傳並儲存 URL。
4.  **最終狀態確認**：將 `tasks` 狀態更新為 `Completed`，打通送給 Gateway 廣播的橋樑。

### 2.4 Agent 衍生機制 (Spawning)
1.  **實作 `DelegateTaskTool`**：寫一個讓 LLM 呼叫的 Function，程式碼邏輯為：建立一個新的子 Task 紀錄 (`Parent Task ID` = 當前 ID)，並重新注入 Gateway 派發迴圈。
2.  **Depth 控制器**：在 `DelegateTaskTool` 內檢查當前 Task Depth (`Depth = Parent Depth + 1`)，超過 3 則拋出 Exception 阻斷 LLM。
3.  **等待機制 (Future / Suspend)**：父 Agent 呼叫衍生後，將自身掛起 (或存入 DB 狀態 `Waiting`)，直到收到子 Agent 的 `TaskCompletedEvent` 才恢復執行。

---

## 階段三：長期記憶進化與通訊推送 (Phase 3 - Evolution & Comm)
本階段目標：實作讓 Agent 持續變聰明的機制 (Vector DB + 動態工具)，並建立完善的前後端通訊。

### 3.1 Vector DB 與 RAG 長期記憶
1.  **啟用 pgvector**：在 PostgreSQL 安裝 pgvector 擴充。
2.  **配置 Vector Store**：設定 Spring AI 的 `VectorStore` 介面，連線至 Postgres。
3.  **實作 Document Retrieval Tool**：建立一個讓 Agent 能主動去 Vector DB 尋找過往記憶或文件的工具。
4.  **實作 Memory Embedding Service**：當 Agent 判斷某些新學到的知識值得被記住時，呼叫此服務將對話或文本 Embedding 並寫入 pgvector。

### 3.2 自主工具加載 (Dynamic Skills)
1.  **實作 `InstallSkillTool`**：提供一組系統工具給高階層的 Agent。
2.  **腳本與 MCP 下載邏輯**：實作讀取遠端 `.md` 或配置檔並下載到本地目錄的邏輯。
3.  **熱重載 (Hot Reload) 機制**：當資料庫 `tools` 表新增後，通知 Gateway 清除對應的權限 Cache 並重新整理 Spring `ApplicationContext` 的工具 Bean。

### 3.3 即時推送與 Internal Event (SSE / Redis Pub/Sub)
1.  **實作 Redis Pub/Sub 廣播器**：當 Gateway 收到 Agent 服務內部發出的 `TaskCompleted` 或 `StatusChanged` 事件時，透過 Redis 發布訊息。
2.  **實作 SSE / WebSocket Controller**：提供前端建立連線的 API (`/api/stream`)。
3.  **前端推播橋接**：監聽 Redis 的訊息，根據訂閱的 Client ID (User ID)，將特定 Task 的最新狀態與產出物 JSON 透過 SSE 推給前端。

---

## 階段四：前端儀表板與視覺化 (Phase 4 - Frontend & UI)
本階段目標：實作終端使用者的操作介面，把後端的狀態轉換為視覺化的樹狀圖與產出報告。

### 4.1 UI 專案建置與介面配置
1.  **建立 React 專案**：Vite + React + TS, 安裝 Tailwind CSS, shadcn/ui。
2.  **實作 Layout**：左側導覽列 (模型設定、Agent 列表、工具管理)、中間主工作台 (對話與任務樹)、右側檢視器 (Task Output Viewer)。

### 4.2 後台控制面板 (Admin & Config UI)
1.  **實作 Model Registry 頁面**：新增、編輯、刪除 AI 模型的清單。
2.  **實作 Agent Profile 頁面**：用於編寫 System Prompt、勾選關聯工具、綁定模型的表單。
3.  **實作 Tool & RBAC 頁面**：管理工具清單與設定哪些群組能存取對應工具。

### 4.3 監控中心與任務分發 UI
1.  **實作 Chat / Input 介面**：讓使用者選定 Agent Profile 並輸入請求，呼叫後端 Task Request API。
2.  **實作 SSE Listener**：前端訂閱後端的 `/api/stream`，透過 Zustand/Redux 維持全域 Task 狀態 (Pending/Running/Completed)。
3.  **實作 Agent 任務狀態卡片**：列出目前所有任務，並根據 SSE 即時改變顏色 (黃 Pending -> 藍 Running -> 綠 Completed)。

### 4.4 Agent 樹狀圖與微觀紀錄檢視 (React Flow)
1.  **導入 React Flow**：將任務列表組成 Node 與 Edge 的結構 (父子關係)。
2.  **動態繪製 DAG 圖**：即時反應 Spawning 的 Agent 子節點，展現 Depth 1, 2, 3 的階層狀態。
3.  **實作 Trace Logs Modal**：點擊單一 Node (Agent)，跳出彈跳視窗，透過 API 拉取該 Node 的 Redis Chat Memory (思考紀錄、工具呼叫歷程)。

### 4.5 產出物多格式檢視器 (Task Output Viewers)
1.  **實作 Viewer Router**：根據接收到的 `Task Output` 格式決定載入的元件。
2.  **實作 Markdown Viewer 模組**：支援 `remark`, `rehype` 與語法高亮 (Syntax Highlighting)。
3.  **實作 Code Diff Viewer 模組**：如同 GitHub PR 的檢視模式，呈現原始碼修改。
4.  **實作資料/圖表 Viewer 模組**：若 Agent 傳回結構化分析結果，結合 Recharts / Chart.js 等套件呈現動態圖表。

---

## 階段五：無伺服器沙盒編譯與執行機制 (Phase 5 - Agent Code Sandbox)
本階段目標：賦予 Agent 撰寫、編譯、執行程式碼並自我修正的能力 (Code Execution Capability)，同時確保宿主機 (Host) 絕對的安全隔離。

### 5.1 Docker 引擎整合與基礎建設
1.  **整合 Docker Client API**：於 Gateway 引入 `docker-java` 工具包，使 Spring Boot 能直接與本機或遠端的 Docker Daemon 溝通。
2.  **建置基礎 Sandbox Image**：打包涵蓋 Java (Maven/Gradle), Python, Node.js 等基礎編譯防護環境的 Docker Image (`teamwork-sandbox-base`)。
3.  **實作 Sandbox Manager Service**：負責 Lifecycle 管理 (Create, Start, Exec, Kill, Delete 容器)。

### 5.2 大模型專用程式碼執行工具 (CodeExecutionTool)
1.  **開發 `@Tool execute_code`**：令 Agent 能夠透過參數傳遞 `language` 與 `source_code` 給這套工具。
2.  **動態掛載與執行**：工具接收到程式碼後，透過 Sandbox Manager 啟動物件容器，掛載暫停的 Volume (或直接 stdin 流寫入檔案)，執行編譯與指令。
3.  **標準輸出入擷取**：捕捉隔離容器內的 `stdout` (執行結果) 與 `stderr` (編譯或執行時期錯誤)，並以字串形式返回給大腦。
    *   *Agent 收到 `stderr` 時觸發 ReAct 的 Self-Correction 機制，重新生成並呼叫工具。*

### 5.3 資源限制與並發效能防護 (Resource Quotas & Pooling)
1.  **防禦性限制 (Limits)**：利用 Docker API 嚴格設定每個沙盒容器的記憶體 (e.g. `256MB`)、CPU (`0.5 cores`) 以及無權限的網路隔離模式 (無對外網路或 Bridge 隔離)。
2.  **容器池化 (Container Pooling)**：為解決高併發情境下 Docker 啟動延遲 (`Docker Cold Start`)，實作一個 Warm Pool。預先在背景啟動 N 個待命的空沙盒。
3.  **安全銷毀與回收**：設定極短的執行 Timeout (例如 30 秒) 以防阻斷式無限迴圈 (Infinite Loop)。任務完成或超時後強制銷毀 (`docker rm -f`) 清理資源。
