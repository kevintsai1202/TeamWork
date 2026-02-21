# Team Work 核心功能需求與動態配置規格 (Feature Requirements)

除了非同步執行任務與長期記憶之外，Team Work 系統必須具備極高的**動態靈活性與權限控管能力**，以支援多租戶 (Multi-tenant) 與高度客製化的 Agent 協作場景。

## 1. 動態模型配置 (Model Agnostic)
**需求描述**：模型可自訂隨意添加。
**架構設計**：
*   **不寫死模型**：系統必須基於 Spring AI 的 `ChatModel` 抽象層介面進行開發，絕不綁定特定廠商 (如 OpenAI 或 Anthropic)。
*   **模型註冊表 (Model Registry)**：在關聯式資料庫 (State DB) 中建立 `ai_models` 表。管理員可以在後台介面隨時新增模型 API Key、Endpoint URL 與 Model Name (例如：`gpt-4o`, `claude-3-5-sonnet`, 或地端部署的 `llama-3`)。
*   **動態載入**：當任務分派給 Agent 時，系統會根據設定的 Model ID，動態實例化對應的 Spring AI `ChatModel` 進行 Inference。

## 2. 工具權限與存取控管 (RBAC for Tools)
**需求描述**：工具可自訂加入，可設定哪些帳號能使用哪些工具。
**架構設計**：
*   **工具註冊表 (Tool Registry)**：在資料庫建立 `tools` 表，記錄系統內建工具、外部 MCP Server、或是動態解析出的 Skills。
*   **多對多關聯 (Account-Tool Mapping)**：建立 `account_tool_permissions` 表，將使用者帳號 (User / Tenant ID) 與工具 ID 進行多對多綁定。
*   **阻斷機制**：在 Agent 準備交握 (Binding) 工具清單給 LLM 之前，Gateway 必須攔截並檢查 `Context Holder` 中的 User ID 是否擁有該工具的存取權限。若無權限，該工具將不會作為 Function Calling 的選項暴露給模型。

## 3. Agent 的完全客製化 (Agent Customization)
**需求描述**：Agent 除了自訂提示詞 (System Prompt)，還能選模型跟工具。
**架構設計**：
*   **Agent 範本定義 (Agent Profile)**：在資料庫中建立 `agent_profiles` 表。這不只是一個單純的字串，而是一個完整的設定檔。
    *   `system_prompt`: 定義角色與職責。
    *   `default_model_id`: 綁定偏好的 LLM 模型 (例如寫 Code 用 Sonnet，總結用 GPT-4oMini)。
    *   `allowed_tool_ids`: 預設掛載的工具陣列。
*   **實例化過程**：當發起任務時，系統根據 `agent_profile_id` 讀取設定，動態組裝：`特定的 ChatModel` + `經過權限過濾的 Tools` + `長期記憶 RAG 注入的 Prompt` = 最終上場執行的 Agent 實體。

## 4. Agent 的自我進化與工具擴充 (Autonomous Skill Acquisition)
**需求描述**：技能或 MCP 可由 Agent 自行加入。
**架構設計**：
*   **這正是我們之前討論的「進化雙引擎」中的核心能力！**
*   **內建 `InstallSkillTool`**：提供一個所有 Agent 預設具備的基礎工具，例如 `InstallMCP` 或 `CreateSkill`。
*   **動態過程**：
    1.  Agent 發現現有工具無法達成任務 (例如需要讀取某個特殊格式的網路日曆)。
    2.  Agent 查閱自身的知識庫，知道有一個 `google-calendar-mcp` 可以用。
    3.  Agent 呼叫 `InstallSkillTool("google-calendar-mcp")`。
    4.  系統在背景下載配置、測試連線並將該 MCP 註冊進系統 (或寫入 `.md` 技能檔)。
    5.  Agent 觸發 `ReloadContext`，下一秒，它的工具列上就多了「操作行事曆」的能力。

## 5. 任務產出資料與前端呈現 (Task Output Presentation)
**需求描述**：除了任務執行狀態 (Pending/Running) 的回報，平台需要妥善儲存並結構化呈現 Agent 所產出的最終資料或中間產物 (Artifacts)。
**架構設計**：
*   **產出物儲存 (Artifact Storage)**：
    *   **結構化格式**：規定 Agent 在產出最後結果時，必須使用特定的結構 (例如 JSON 格式的 `TaskResult` 物件，包含 `summary`, `content`, `artifacts`, `metrics`)，而不只是單純的聊天文字。
    *   **持久化**：Gateway 在接收到 `Completed` Internal Event 時，會將這些產出與原始 Task ID 綁定，結構化地存入 State DB，若是大型檔案 (如圖片、程式碼壓縮檔) 則存入 MinIO / AWS S3，DB 僅紀錄關聯 URL。
*   **前端動態渲染 (Dynamic UI Rendering)**：
    *   前端接收到任務完成的 SSE 推播後，根據產出物的 `MIME Type` 或自訂標籤動態切換檢視元件 (Viewers)。
    *   **文字與報告**：提供 Markdown 渲染器，支援語法高亮。
    *   **程式碼與修改檔 (Diffs)**：提供類似 GitHub 的檔案比較與程式碼檢視元件。
    *   **多媒體檔案**：提供圖片預覽或下載按鈕。
    *   **互動式元件**：若 Agent 產出的是特定格式的 JSON，前端可直接渲染為圖表 (Charts) 或是可互動的表單 (如調查問卷)。
