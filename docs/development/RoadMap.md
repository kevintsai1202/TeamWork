# 專案 RoadMap：Team Work 多 AI Agents 協作系統

## 技術棧
*   **後端框架**：Spring Boot 4 (4.0.3) + Java 21 (啟用 Virtual Threads)
*   **AI 引擎**：Spring AI 2.0.0-M2 (支援 OpenAI, Claude 等並提供 Function Calling)
*   **持久化儲存**：PostgreSQL (系統狀態), Redis (Chat Memory 分散式快取)
*   **前端框架**：React + TypeScript + Tailwind CSS
*   **視覺化套件**：React Flow 或 D3.js (用於 Agent 樹狀圖)
*   **工具擴充**：Spring AI Agent Utils, Tool Integration (包含 MCP, 社群工具, 自定義 Skills)

## 功能規劃

### 核心基礎建設 (Phase 1)
- [ ] **Spring Boot Gateway 初始化**：建立 REST API 接收前端任務 (TaskID 生成)。
- [ ] **狀態資料庫與 Chat Memory**：實作隔離上下文的儲存機制 (StateDB與緩存)。
- [ ] **基礎 Agent ReAct 迴圈**：實作 Master Agent (Depth 0) 的呼叫與非同步執行緒池 (@Async)。
- [ ] **Internal Event 回報機制**：實作 Agent 完成後的 Callback 處理以及基於 SSE 的前端推播。

### Agent 動態裝配與權限控管 (Phase 2)
- [ ] **Model & Agent Registry**：在 PostgreSQL 建立 `models`, `tools`, `agent_profiles` 表，支援介面動態新增模型與組裝 Agent (Prompt + Model + Tools)。
- [ ] **RBAC 權限控管**：在 Agent 載入 Function Calling 之前，透過帳號 (TenantID) 過濾禁止使用的工具。
- [ ] **自主進化引擎 (Autonomous Tools)**：實作 `InstallSkill` 內建工具，允許 Agent 在執行期動態安裝/寫入新的 MCP Server 或 `.md` Skills，並即時 Reload 到實例中。

### 前端儀表板開發 (Phase 3)
- [ ] **主控台 UI 雛型**：React 專案初始化，實作工作台配置。
- [ ] **SSE 即時狀態監控**：對接後端推播，更新 Task 狀態 (Pending -> Running -> Completed)。
- [ ] **視覺化 Agent 樹狀圖**：使用 React Flow 動態繪製任務拆解的主/子 Agent 連線關係。
- [ ] **Trace Logs 檢視**：點擊個別節點可檢視背後的思考日誌 (Thinking Logs) 與工具使用紀錄。
- [ ] **多格式產出呈現 (Output Viewers)**：實作不同的渲染元件 (如 Markdown, Code Viewer, 資料圖表)，以結構化方式呈現 Agent 完成任務後產出之最終 Artifacts。

## 里程碑
- [ ] **Milestone 1: 概念驗證 (PoC)** - 完成單一 Agent 非同步調用與 Internal Event 回傳，前端能接收 SSE 推播並顯示。
- [ ] **Milestone 2: 擴充力展示** - 成功掛載外部 MCP 服務 (如 Firecrawl)，並驗證 Agent 能正確使用外部工具。
- [ ] **Milestone 3: Agent 衍生驗證** - 成功讓 Master Agent 根據問題複雜度拆解任務，自我衍生 Sub Agent。
- [ ] **Milestone 4: MVP 上線** - 完整的視覺化儀表板上線，使用者可以直觀操作整個多 Agent 分派流程。

---
*備註：本 RoadMap 基於 OpenClaw 架構概念修改，採 Internal Event 機制提高可靠性，並引入 Spring AI Agent Utils 作為工具層標準。*
