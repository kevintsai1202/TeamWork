# Team Work: 多 AI Agents 協作系統架構規劃

## 1. 系統核心概念與目標
**Team Work** 是一個基於 OpenClaw 概念設計的多 Agent 協作平台。主要目標為：
*   **非同步與去中心化**：Agent 執行任務後，系統能立即返回主控畫面，不阻塞使用者操作。
*   **自訂 Agent 與模組化技能**：可依需求定義不同種類的 Agent (如 Researcher, Coder)，並動態賦予特定技能 (Tools)。
*   **獨立上下文管理**：支援同一種類 Agent 的多次、並發呼叫，確保每次實例都有獨立的 Conversation Context。
*   **自主任務分派 (Spawning)**：Agent 在面臨複雜任務時，可自主衍生子 Agent 進行處理。
*   **層級防護機制**：為避免無限迴圈與資源耗盡，Agent 衍生深度最高限制為 **三層 (Depth = 3)**。
*   **視覺化與即時監控**：提供儀表板即時監控所有 Agent 的執行狀態，並以樹狀結構呈現 Agent 之間的衍生關係。

## 2. 技術棧選型
*   **後端框架**：Spring Boot 4.x (4.0.3) (建議 Java 21+ 啟用 Virtual Threads 以處理高併發 I/O)。
*   **AI 整合**：Spring AI 2.0.0-M2 (介接 LLM、管理 Chat Memory、提供 Function Calling)。
*   **前端框架**：React + TypeScript。
*   **狀態與通訊 (借鑒 OpenClaw)**：
    *   **後端對外**：使用 **Internal Event (Callback)** 作為 Agent 完成任務或觸發事件的非同步回報機制。處理耗時長、非同步的 AI 任務時具備極高韌性與彈性。
    *   **對前端 UI**：前端可同時接受 WebSocket/SSE 推播事件（由 Gateway 收到 Internal Event 後轉發），維持監控面板的即時性。
*   **UI 呈現**：React Flow 或 D3.js 用於繪製 Agent 任務樹與關聯圖。

## 3. 核心機制設計

### A. 獨立上下文與狀態管理 (Stateful Context)
*   **TaskInstanceID**：每次呼叫 Agent 都會生成唯一的 Task ID。
*   **Conversation Memory**：結合 Spring AI 的 Chat Memory 機制，利用 TaskInstanceID 將關聯的上下文儲存於 DB 或 Redis。
*   確保如「同時呼叫十個 Researcher Agent」的場景下，彼此記憶完全隔離。

### B. 任務分派與非同步迴圈 (Async ReAct Loop)
*   採用事件驅動架構 (Event-Driven Architecture) 與 **Internal Event 回呼機制**。
*   當使用者 (主線) 派發任務：
    1.  Gateway 接收請求，建立 Task 紀錄 (狀態為 `Pending`)。
    2.  將任務丟入背景執行緒 (Spring `@Async` / Virtual Threads)，狀態轉為 `Running`。
    3.  立刻返回 HTTP 200 給前端，釋放主執行緒。
*   **為什麼使用 Internal Event 而不是單純的 WebSocket / SSE？**
    *   **真正的非同步與解耦**：AI 任務 (如爬蟲、讀取千份檔案) 時間極不可預期。若使用 WebSocket 必須維持長連接，資源耗損大且易受網路波動影響（斷線重連遺失狀態）。
    *   **高可用性 (Stateless & HA)**：Agent 完成任務後，只需將結果打包打一個 Internal Event 給 Gateway。即使 Gateway 伺服器中途重啟，也完全不影響背景 Agent 的執行。
    *   **易於系統整合**：容易與 GitHub Actions、Slack 乃至其它第三方自動化服務串接。
*   Agent 在背景利用 ReAct 迴圈執行工具，產出最終結果後，觸發 Internal Event 通知 Gateway：
    *   **狀態更新**：將 Task 狀態轉為 `Completed`。
    *   **結構化產出 (Artifacts)**：Agent 將最終報告、程式碼或 JSON 結構化資料回傳。Gateway 會將這些產出與 TaskID 綁定存入關聯式資料庫，大型檔案則存放於物件儲存 (如 AWS S3 / MinIO)。
    *   **前端渲染**：Gateway 利用 WebSocket 將更新推播給儀表板。前端根據產出物的格式 (MIME Type) 動態載入 Markdown Viewer、Code Diff Viewer 或 Chart 等合適的渲染元件。

### C. Tool Integration (工具擴充與整合)
*   **模組化 Skills 系統**：基於 `Spring AI Agent Utils` 的設計模式，將各類系統能力（如內建函數、MCP 伺服器、外部 API、本地腳本）統一包裝為標準化的 **Skills** 工具庫。
    *   **Skill 目錄結構**：每個能力被封裝成一個包含 `.md` (文件說明) 與設定檔的目錄，利用 `SkillsTool.builder().addSkillsResources()` 動態掛載給 Agent。
*   **Tool Client 與 MCP 介接**：利用 Spring AI 相容性，將標準化的 Tool Client (含內建功能與 MCP 協定) 封裝進 Agent 的回呼 (Callbacks) 中。
    *   Agent (LLM) 不需要知道底層網路實作，只需要呼叫抽象的 Tool Interface。
    *   系統可根據 Agent 的角色配置，在執行個體建立時動態裝載 (`Dynamic Tool Loading`) 指定的 Tool Integration 清單。

### D. 動態模型配置與權限控管 (Dynamic Registry & RBAC)
*   **多模型支援 (Model Agnostic)**：系統不綁定單一大型語言模型。可於關聯式資料庫建立 Model Registry，管理 API Keys 與 Endpoints，支援如 `gpt-4o`, `claude-3-5-sonnet`, 或地端 `llama-3` 等模型切換。
*   **Agent 實例化配置 (Agent Profiles)**：不再硬體編碼 (Hardcode) Agent 的行為。從資料庫讀取 Agent 設定，包含：`特定的 System Prompt` + `偏好的 ChatModel` + `被允許使用的 Tools 陣列`。
*   **帳號工具權限 (RBAC for Tools)**：Gateway 在實例化 Agent 前，會檢查目前觸發任務的 User ID / Tenant ID，過濾使用者無權呼叫的工具，確保多租戶間的權限隔離。
*   **自主進化引擎 (Autonomous Tools)**：內建特殊的 `InstallSkillTool`。允許 Agent 在發現現有工具不足時，主動尋找並掛載全新的 `.md` 技能或 MCP Server，實現系統能力的熱更新 (Hot Reload)。

### D. Agent 自主分派機制與三層限制 (Agent Spawning)
*   將「分派任務給另一個 Agent (`DelegateTask`)」定義為一個 LLM Tool / Function。
*   **深度控制 (Depth Control)**：
    *   主線發起的任務 Depth = 0。
    *   當 LLM 呼叫 `DelegateTask` 時，後端擷取當前 Depth，若 Depth < 3，則允許建立新 Task (Depth = 當前 Depth + 1)。
    *   若 Depth == 3，後端直接在 Tool Response 中回傳錯誤訊息 (例如："Maximum delegation depth reached. You must complete the task yourself.")，強制該 Agent自行收斂結果。

### D. 動態回報路徑 (Hierarchical Callbacks)
*   建立 Task 時記錄 `ParentTaskId`。
*   當 Agent (Task) 完成時觸發 `TaskCompletedEvent`：
    *   **無 Parent (主線任務)**：將結果透過 WebSocket 廣播至前端儀表板，通知使用者。
    *   **有 Parent (子/孫任務)**：喚醒休眠中 (或等待中) 的 Parent Agent，將子任務的結果作為 `DelegateTask` 的 Return Value 注入 Parent 的上下文中，讓 Parent 繼續推理。

### E. 前端監控儀表板 (Dashboard)
*   實時顯示 Agent 狀態 (`Pending`, `Running`, `Completed`, `Failed`)。
*   視覺化展示 Agent 衍生關係，如：`主控端 -> Researcher (Depth 1) -> SearchTool (Depth 2)`。
*   使用者可點擊樹狀圖節點，檢視該 Agent 的對話歷程、思考脈絡 (Thinking Logs) 以及最終產出。

## 4. 潛在挑戰與防禦措施
*   **無窮迴圈與資源耗盡**：透過嚴格的 Depth 限制、Token / Cost 預算控制、以及執行 Timeout 設定來預防。
*   **併發效能**：依賴 Java 21 的虛擬執行緒解決大量 HTTP 呼叫 (給 LLM API) 造成的阻塞問題。
*   **日誌與透明度**：參考 OpenClaw 設計，將 Agent 的思考過程與記憶寫入文件或可視化的結構中，提升除錯效率與系統透明度。
