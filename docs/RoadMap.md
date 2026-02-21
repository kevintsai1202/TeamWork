# 專案 RoadMap：Team Work 多 AI Agents 協作系統

> **專案願景**：打造一個具備「非同步執行」、「動態工具擴充 (Tool Integration)」、「自主衍生 (Spawning)」與「長期記憶 (Vector DB)」的多 Agent 協作平台。

## 🛠️ 技術棧
*   **後端框架**：Java 21, Spring Boot 4 (4.0.3) (啟用 Virtual Threads)
*   **AI 代理引擎**：Spring AI 2.0.0-M2 (支援 Function Calling 與跨模型支援)
*   **持久化儲存**：PostgreSQL (任務狀態) + pgvector (長期記憶)
*   **快取與通訊**：Redis (Chat Memory, Pub/Sub Internal Events)
*   **前端應用**：React + TypeScript + Tailwind CSS (包含 React Flow 視覺化任務樹)

## 🗺️ 階段性功能規劃 (Phases)

### Phase 1: 核心基礎與任務引擎 (Foundation)
- [ ] 基礎 Spring Boot API 與 Postgres/Redis 連線架構
- [ ] Agent 狀態機 (Pending -> Running -> Completed/Failed)
- [ ] Master Agent (Depth 0) 的單點非同步執行迴圈 (ReAct Loop)
- [ ] 基於 Redis Pub/Sub 與 SSE 的事件推播與前端通訊

### Phase 2: 動態裝配與權限引擎 (Dynamic Assembly)
- [ ] Model & Agent Registry：建立資料庫，支援動態組裝 Agent (結合特定模型與 Prompt)。
- [ ] RBAC 工具權限：實作多租戶的 Function Calling 權限過濾。
- [ ] Tool Integration：實作模組化 Skills 系統，支援內建工具、MCP Server 與 `.md` 技能解析。

### Phase 3: 自主進化與衍生機制 (Evolution & Spawning)
- [ ] 導入 Vector DB (pgvector) 提供 Agent 檢索過去任務歷史的長期記憶。
- [ ] 實作 `InstallSkillTool` 讓 Agent 發現工具不足時能自動自動掛載新技能。
- [ ] 實作 Agent 衍生能力 (`DelegateTask`)，允許拆解任務並生成子 Agent (限制深度 3 層)。

### Phase 4: 前端任務儀表板 (Dashboard UI)
- [ ] 即時狀態監控看板：透過 SSE 接收並顯示任務狀態變化。
- [ ] 任務關係樹狀圖：利用 React Flow 視覺化呈現主/子 Agent 的調用路徑。
- [ ] 任務產出檢視器 (Task Output Viewers)：動態渲染 Agent 產出的 Markdown, 程式碼 Diffs 與結構化資料。

## 🚩 里程碑 (Milestones)
- [ ] **Milestone 1 (PoC)**：單一 Agent 成功於背景執行，並透過 Internal Event 將結果推播回前端。
- [ ] **Milestone 2 (MVP)**：支援多重 Tool Integration 與基本的 Agent 動態設定，具備簡易 Dashboard 介面。
- [ ] **Milestone 3 (V1.0)**：完整實作 Agent 衍生機制 (Spawning)、自主工具加載，並上線任務視覺化樹狀圖。

---
*備註：詳細的技術實作步驟與開發順序，請參見 `docs/development/Development_Plan.md`。*
