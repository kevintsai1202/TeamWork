# Team Work 多 AI Agents 協作系統架構圖

本架構圖展示了 **Team Work** 系統的完整運作流程，涵蓋了從前端監控面板到後端 Gateway、Agent 背景執行 (ReAct 迴圈)、Agent Spawning (自主衍生)，以及透過 MCP 介接外部工具的詳細機制。

```mermaid
graph TD
    %% 樣式定義：設定清晰的字體與顏色
    classDef frontend fill:#E3F2FD,stroke:#1565C0,stroke-width:2px,color:#0D47A1,font-size:16px,font-weight:bold;
    classDef gateway fill:#E8F5E9,stroke:#2E7D32,stroke-width:2px,color:#1B5E20,font-size:16px,font-weight:bold;
    classDef agent fill:#FFF3E0,stroke:#E65100,stroke-width:2px,color:#E65100,font-size:16px,font-weight:bold;
    classDef external fill:#FCE4EC,stroke:#C2185B,stroke-width:2px,color:#880E4F,font-size:16px,font-weight:bold;
    classDef db fill:#F3E5F5,stroke:#6A1B9A,stroke-width:2px,color:#4A148C,font-size:16px,font-weight:bold;
    classDef mcp fill:#E0F7FA,stroke:#00838F,stroke-width:2px,color:#006064,font-size:16px,font-weight:bold;

    %% -------------------
    %% 1. 前端 UI (React)
    %% -------------------
    subgraph 前端儀表板
        UI_Dashboard["即時監控面板 (顯示 Agent 狀態與樹狀圖)"]:::frontend
        UI_TaskInput["任務輸入介面 (發送初始任務)"]:::frontend
    end

    %% -------------------
    %% 2. 核心 Gateway (Spring Boot)
    %% -------------------
    subgraph 後端_Gateway
        API_Controller["REST API 控制器 (接收任務 & 產生 TaskID)"]:::gateway
        EventManager["Event Broker (轉發狀態給前端)"]:::gateway
        Internal Event_Endpoint["Internal Event Endpoint (接收 Agent 非同步回報)"]:::gateway
    end

    %% -------------------
    %% 3. 持久化與資料中樞 (Redis & DB)
    %% -------------------
    subgraph 關聯式架構
        DB_State["Postgres DB (核心狀態 & pgvector 長期記憶)"]:::db
    end

    subgraph Redis_大腦與神經網路
        Event_Bus["Redis Streams (Event Bus 訊息匯流排)"]:::db
        Chat_Memory["Redis Chat Memory (上下文暫存大腦)"]:::db
    end

    %% -------------------
    %% 4. 背景虛擬執行緒池
    %% -------------------
    subgraph Agent_執行池
        
        %% 主線 Agent (Depth 0)
        Agent_Master["Master Agent Depth 0 (Spring AI)"]:::agent
        ReAct_Master{"ReAct 迴圈"}:::agent
        Agent_Master --- ReAct_Master
        
        %% 子 Agent (Depth 1)
        subgraph Agent_自主衍生
            Agent_Child["Sub Agent Depth 1 (委派任務)"]:::agent
            ReAct_Child{"ReAct 迴圈"}:::agent
            Agent_Child --- ReAct_Child
        end
    end

    %% -------------------
    %% 5. 擴充技能與 Tool Integration
    %% -------------------
    subgraph 工具與技能整合_Tool_Integration
        Skills_Dir["Skills Directory (自定義 .md 提示詞)"]:::mcp
        MCP_Client["MCP Server (官方/社群擴充工具)"]:::mcp
        BuiltIn_Tools["Built-in Tools (系統內建工具)"]:::mcp
        External_Services["外部 API 與服務 (GitHub, 搜尋等)"]:::external
    end

    %% ===================
    %% 流程關聯連線
    %% ===================

    %% 任務派發流程
    UI_TaskInput -->|1. 發送新任務 (含 userId)| API_Controller
    API_Controller -->|2. 狀態 PENDING| DB_State
    API_Controller <-->|3. (動態設定) 查詢許可模型與工具| DB_State
    API_Controller -->|4. 依據配置動態組裝並派發任務| Agent_Master
    API_Controller -.->|5. 回傳 HTTP 200| UI_TaskInput
    
    %% Agent 執行與記憶體存取
    Agent_Master <-->|讀寫專屬上下文| Chat_Memory
    Agent_Child <-->|讀寫專屬上下文| Chat_Memory

    %% Agent Spawning (自主任務委派)
    ReAct_Master -->|5a. 呼叫 DelegateTask 檢查 Depth| Agent_Child
    ReAct_Child -->|6. 完成後回傳執行結果| ReAct_Master

    %% 工具調用 (Tool Integration)
    ReAct_Master <-->|調用 Tools / Skills| Skills_Dir
    ReAct_Master <-->|調用內建 Tools| BuiltIn_Tools
    ReAct_Child <-->|調用特定 Tools| Skills_Dir
    Skills_Dir <--> MCP_Client
    MCP_Client <-->|透過 MCP 通訊| External_Services

    %% 多租戶事件匯流排回報流程 (Redis Streams)
    ReAct_Master -->|7. 發布狀態/結果 (Pub)| Event_Bus["Redis Streams (Event Bus)"]
    Agent_Child -->|發布子任務狀態 (Pub)| Event_Bus
    Event_Bus -->|8. 訂閱事件 (Sub)| EventManager
    EventManager -->|9. 更新總狀態| DB_State

    %% 前端推播 (多租戶與大盤)
    EventManager -->|10a. WebSocket (依 userId)| UI_Dashboard
    EventManager -->|10b. WebSocket (全域廣播)| UI_Central["中央監控儀表板 (全局綜觀)"]:::frontend
```
