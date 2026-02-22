# TeamWork 文件任務清單

## 任務狀態
- [x] T1：建立並定版 `docs/spec.md`（系統規格與 12 類圖）【已完成】
- [x] T2：建立並定版 `docs/api.md`（RESTful API 規格）【已完成】
- [x] T3：文件一致性檢查與收尾（`spec/api/todolist` 對齊）【已完成】
- [x] T4：修正 `gateway` 新版 Spring Boot 的 Testcontainers 依賴並驗證編譯【已完成】
- [x] T5：升級 `gateway` 的 Spring AI 版本至 2.0.0-M2 並驗證編譯/測試【已完成（編譯通過；整合測試受限於本機 Docker 環境）】
- [x] T6：整合測試改為直連 `docker-compose` PostgreSQL（移除 Testcontainers 依賴）並驗證【已完成】
- [x] T7：讓 `MasterAgent` 支援 Spring AI Tool Calling 並驗證編譯測試【已完成】
- [x] T8：建立 DB/記憶體 Tool Registry，讓 `MasterAgent` 每次任務動態載入工具並支援熱切換【已完成】
- [x] T9：改為純動態 AI 設定（移除 OpenAI starter）並於啟動時將 `.env` 寫入 `ai_models` 作預設模型【已完成】
- [x] T10：引入 `spring-ai-community/spring-ai-agent-utils` 依賴，作為下一階段 Sub Agent 開發基礎【已完成】
- [x] T11：參考 `spring-ai-agent-utils` GitHub 範例，導入最小 Sub Agent 委派骨架並驗證編譯測試【已完成】
- [ ] T12：實作 Master 依 sub-agent 描述自主路由（自動決定呼叫哪個 agent）並補齊測試【待辦】
- [x] T13：建立統一 Agent Provider 介面（一般 Agent / Claude SDK 同接口呼叫）並完成整合【已完成】
- [ ] T14：串接 `agent-sandbox` 作為可控執行沙盒，並接入統一 Agent Provider 流程【待辦】
- [ ] T15：串接 `agent-client` 作為外部 Agent 呼叫通道（透過 provider adapter）【待辦】
- [ ] T16：比對 `spring-ai-agent-utils/examples`，補齊現有缺口能力（僅缺口，不重做已完成功能）【待辦】
- [x] T17：依指定基線重整 `docs/RoadMap.md`，並同步 `docs/development` 文件（spec/api/todolist）【已完成】

## 下一階段差距拆分（發布基線）

### T12：Master 依 sub-agent 描述自主路由
- [ ] T12-1：建立 `SubAgentDescriptorRepository`，集中管理 name/description/tools/owner-provider。
- [ ] T12-2：新增 `SubAgentRouter`，依任務語意與 descriptor 打分決策（至少含 fallback 規則）。
- [ ] T12-3：`MasterAgent` 改為「先路由、後執行」，並保留無匹配時的 `default` 路徑。
- [ ] T12-4：補齊路由測試（命中/未命中/多候選平手）與回歸測試。

### T14：串接 agent-sandbox
- [ ] T14-1：建立 `SandboxExecutionProvider`（adapter）並接 `agent-sandbox`。
- [ ] T14-2：在 `UnifiedAgentProvider` 流程中加入 sandbox capability 檢查與 timeout/cancel 控制。
- [ ] T14-3：新增安全保護（允許語言白名單、最大執行時間、輸出大小上限）。
- [ ] T14-4：補齊整合測試（成功執行、超時、拒絕語言、執行失敗回報）。

### T15：串接 agent-client
- [ ] T15-1：新增 `AgentClientUnifiedAgentProvider`，實作外部 agent 呼叫。
- [ ] T15-2：補齊 provider 選擇策略（依 `ai_models.provider` + endpoint 配置）。
- [ ] T15-3：加入重試與錯誤分類（連線錯誤/4xx/5xx）及可觀測性欄位。
- [ ] T15-4：補齊契約測試與降級策略（外部失敗時 fallback）。

### T16：比對 examples 補缺口
- [ ] T16-1：建立 `examples-gap-matrix`（能力、現況、缺口、優先級）。
- [ ] T16-2：優先補齊高價值缺口：多 sub-agent 編排、tool callback 組態化、錯誤回傳標準化。
- [ ] T16-3：補齊文件對齊（`spec.md` / `api.md` / `RoadMap.md` / `todolist.md`）。

## 執行紀錄
- 2026-02-21：建立任務清單，開始 T1。
- 2026-02-21：完成 T1，開始 T2。
- 2026-02-21：完成 T2，開始 T3。
- 2026-02-21：完成 T3，文件對齊完成。
- 2026-02-21：開始 T4，先更新 `spec.md`、`api.md` 的測試依賴基線，再進行程式修正。
- 2026-02-21：完成 T4，已補齊 Testcontainers 依賴、修正 Spring Boot 4 / Spring AI 測試相容性，`mvn clean test-compile` 通過。
- 2026-02-21：開始 T5，升級 Spring AI 至 2.0.0-M2 並進行相容性驗證。
- 2026-02-21：T5 驗證受阻，`mvn clean test-compile` 顯示 Spring AI 2.x 破壞性 API 差異（`OpenAiApi`、`OpenAiChatModel`、`MessageChatMemoryAdvisor`、`ChatMemory` 介面、`Message` 內容取得方式）。
- 2026-02-21：完成 T5，已完成 Spring AI 2.x 相容性修正並通過 `mvn clean test-compile`；`mvn test` 僅剩 Testcontainers 整合測試因本機缺少可用 Docker 環境失敗。
- 2026-02-21：開始 T6，整合測試改為使用 `docker-compose` DB，不再依賴 Testcontainers。
- 2026-02-21：完成 T6，已移除 Testcontainers 依賴與註解，整體測試 `mvn test` 通過（27/27）。
- 2026-02-21：開始 T7，為 `MasterAgent` 接入可調用工具（Tool Calling）能力。
- 2026-02-21：完成 T7，新增 `MasterAgentTools` 並於 `MasterAgent` 掛入 `.tools(...)`，`mvn test` 通過（27/27）。
- 2026-02-21：開始 T8，導入動態工具註冊表（DB+記憶體快取）與任務級工具載入流程。
- 2026-02-21：完成 T8，新增 `DynamicToolRegistry`、`MasterAgent` 改為每次任務動態取工具，並通過 `mvn test`（30/30）。
- 2026-02-21：開始 T9，改為純動態 AI 設定，並新增 `.env` 啟動初始化預設模型流程。
- 2026-02-21：完成 T9，移除 OpenAI starter 自動配置依賴、改用 `spring-ai-openai` + `spring-ai-client-chat`，並於啟動時將 `.env` 同步到 `ai_models` 作預設模型，`mvn test` 通過（30/30）。
- 2026-02-21：開始 T10，先引入 `spring-ai-agent-utils` 依賴，下一階段接續實作 Sub Agent。
- 2026-02-21：完成 T10，已引入 `org.springaicommunity:spring-ai-agent-utils:0.4.2` 並通過編譯驗證。
- 2026-02-21：開始 T11，將以 `TaskToolCallbackProvider` 與 `ClaudeSubagentReferences` 套用最小可用 Sub Agent 作法。
- 2026-02-21：完成 T11，`MasterAgent` 已掛載 `TaskToolCallbackProvider` 與 markdown 子代理定義（`agents/subagents/general-researcher.md`），`mvn test` 全數通過（30/30）。
- 2026-02-21：新增 T12，規劃讓 Master 根據 sub-agent 的 name/description/tools 自主選擇委派對象，並要求每個 sub-agent 維持獨立上下文。
- 2026-02-21：開始 T13，新增統一 Agent Provider 抽象層，讓不同來源 Agent 可用同一呼叫接口整合。
- 2026-02-21：完成 T13，新增 `UnifiedAgentProvider`/`UnifiedAgentRegistry`，`MasterAgent` 改由統一接口呼叫；保留 `SpringAiUnifiedAgentProvider` 與 `ClaudeSdkUnifiedAgentProvider`（佔位），`mvn test` 通過（30/30）。
- 2026-02-21：新增後續規劃基線：Sandbox 採 `agent-sandbox`、外部 Client 採 `agent-client`、工具調用採 `spring-ai-agent-utils(+examples)`；已完成能力不重做。
- 2026-02-21：完成 T17，已依指定來源重整 `docs/RoadMap.md`，並同步更新 `docs/development/spec.md`、`docs/development/api.md`、`docs/development/todolist.md`。
- 2026-02-22：完成待辦差距盤點，已將 T12/T14/T15/T16 拆分為可執行子任務，作為下一版發布基線。
