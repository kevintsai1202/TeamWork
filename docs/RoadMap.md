# 專案 RoadMap：Team Work 多 Agent 協作系統（2026-02-23 同步版）

> 專案願景：打造可多租戶、多 Agent、可擴充工具、可接外部 Agent SDK 的協作平台，且維持統一呼叫介面與可控執行安全邊界。

## 技術基線
- 後端：Java 21 + Spring Boot 4.0.3
- AI：Spring AI 2.0.0-M2
- 儲存：PostgreSQL + Redis
- Agent 統一介面：`UnifiedAgentProvider` / `UnifiedAgentRegistry`

## 指定整合來源（固定）
- Sandbox：`https://github.com/spring-ai-community/agent-sandbox`
- 外部 Agent Client：`https://github.com/spring-ai-community/agent-client`
- Agent 工具調用：`https://github.com/spring-ai-community/spring-ai-agent-utils`
- 工具範例：`https://github.com/spring-ai-community/spring-ai-agent-utils/tree/main/examples`
- 原則：已完成能力不重做，只補缺口整合

## 階段規劃與現況
### Phase A：已完成基礎（Done）
- [x] Spring AI 2.x 升級與相容修正
- [x] 測試改為直連 docker-compose（不依賴 Testcontainers）
- [x] Master Agent Tool Calling
- [x] 動態工具開關（DB + 記憶體快取）
- [x] 純動態模型配置（`.env` 啟動寫入 DB）
- [x] Sub Agent 最小骨架（`spring-ai-agent-utils`）
- [x] 統一 Agent 介面（一般 Agent / SDK Agent 同接口）

### Phase B：當前開發（In Progress）
- [x] Master 依 sub-agent 描述自主路由（T12）  ✅ 已完成，含 AI+關鍵字混合路由與測試

### Phase C：下一步整合（Next）
- [x] 串接 `agent-sandbox` 作為可控執行沙盒（T14, MVP）✅ 已完成
- [x] T18（主線）：納入 `spring-ai-agent-utils` 全量能力（不含 A2A，已完成）
- [x] T19（關鍵）：上下文治理與壓縮（每個 agent 用量可視化、完整上下文檢視、手動刪除、自動壓縮閾值；已完成 T19-1/T19-2/T19-3/T19-4/T19-6/T19-7）
- [x] T20（關鍵）：排程治理（可自由調整、多排程、指定 Agent/Tool/Skill、共享排程上下文；已完成 T20-1~T20-8）
- [x] T21（關鍵）：通知編排（Email/社群通知，並可於排程/觸發選擇通知策略；MVP `EMAIL/WEBHOOK` 已完成）
- [ ] T15（分段）：串接 `agent-client` 作為外部 Agent 呼叫通道（T15-A 非CLI先行，T15-B CLI後置）
- [ ] T16：併入 T18，不再獨立執行

> 執行順序（強制）：先完成 T18/T19（含測試通過）→ 再推進 T20/T21 → 最後處理 T15。
> 品質門檻（強制）：每個子任務完成後，必須測試通過才可進入下一子任務。

#### T18/T19 並行泳道（Parallel Lanes）
- **Lane A（路由主線，已完成）**：T18-4 單一路由真相（routing source of truth）。
- **Lane B（觀測主線，已完成）**：T18-7 觀測面（metrics/log/trace）與錯誤分類。
- **Lane C（上下文資料主線，已完成）**：T19-1（用量統計）+ T19-3（完整上下文檢視 API）。
- Lane A 交付：路由入口統一、回歸測試通過。
- Lane B 交付：指標/日誌/追蹤欄位與錯誤分類落地、測試通過。
- Lane C 交付：上下文用量查詢與完整檢視 API 落地、測試通過。
- **Gate G1（合併閘點，已達成）**：Lane A/B/C 都完成且測試通過，已可進入 T19-2（自動壓縮）。
- **Gate G2（收斂閘點，已達成）**：T19-2、T19-4、T19-6、T19-7 已完成且測試通過，已可開啟 T20 主流程。

#### T18（Agent Utils 全量納入，不含A2A）
- [ ] 建立能力矩陣與缺口盤點，對齊 `spring-ai-agent-utils` 與 `examples` 的可用能力。
- [x] 完成 skills 動態更新機制，確保工具與技能可在不重啟情況下刷新。
- [x] 納入 ask-user-question 互動能力，讓代理在必要時可安全回問使用者。
- [x] 整併為單一路由真相（single source of routing truth），避免多路由策略分叉。
- [x] 補齊安全治理（RBAC、sandbox 邊界）基線，並完成預設工具政策控管。
- [x] 補齊可觀測性（metrics/log/trace）與相容層策略，降低整合風險。
- [x] 完成相容層（compatibility layer）與回歸驗證（legacy provider alias 正規化 + 回歸測試基線）。

#### T14（MVP）範圍收斂
- 先完成最小可用：`SandboxExecutionProvider` 接通、timeout、語言白名單、錯誤回傳標準化。
- 暫不納入：多層 fallback 編排、細粒度策略引擎、完整審計報表（移至後續版本）。

#### T19（上下文治理與壓縮）
- 預設啟用自動上下文壓縮，支援 token 閾值與壓縮目標設定。
- 提供每個 agent/task 的上下文用量指標（messageCount、estimatedTokens、compressionCount）。
- 提供完整上下文檢視與手動刪除能力（單筆/區間/摘要/全歷史）並保留審計軌跡。
- 當前進度：已完成 `T19-1`（用量統計）、`T19-2`（自動壓縮）、`T19-3`（完整檢視）、`T19-4`（手動刪除 + 審計）、`T19-6`（觀測事件/指標）、`T19-7`（壓縮模板與回歸測試）。
- 並行建議：先並行完成 `T19-1` + `T19-3`，再進入 `T19-2` 與 `T19-6`，最後收斂 `T19-4`、`T19-7`。

#### T15（agent-client 分段策略）
- T15-A（非CLI先行）：先完成 provider adapter 骨架、路由策略、重試/錯誤分類、契約測試與降級。
- T15-B（CLI後置）：再接 CLI transport（命令映射、權限、雲主機維運腳本）。
- 原則：先確保 `UnifiedAgentProvider` 介面與非 CLI 路徑可用，CLI 僅作為可插拔 transport。

#### T20（排程治理與共享上下文）
- 當前進度：已完成 `T20-1`（排程資料模型：`task_schedules`、`schedule_runs`、`schedule_context_snapshots`、`task_triggers`）、`T20-2`（排程管理 API）、`T20-3`（多排程執行器 + 動態重載 trigger）、`T20-4`（執行目標路由 + 統一錯誤分類）、`T20-5`（共享上下文模式快照讀寫）、`T20-6`（上下文保留/壓縮策略）、`T20-7`（排程觀測快照與事件計數）、`T20-8`（多排程並行/共享上下文續跑/即時生效回歸測試）。
- 已補舊系統單向 webhook 入口（`POST /triggers/webhooks/{webhookKey}`）：受理後回 `202`，非同步交由 Agent 執行。
- 舊系統整合採「單向事件入口」：舊系統僅透過 webhook/trigger 將事件送入 TeamWork，TeamWork 僅回覆接收結果（`202 Accepted`），不阻塞舊系統流程。
- 排程管理需支援建立/更新/啟用/停用/立即執行，且更新後可即時生效。
- 同租戶支援多排程並行，具備併發與優先級控制。
- 已落地 runtime scheduler：啟動時重載啟用中的 CRON/INTERVAL 排程，且在建立/更新/啟停/刪除後即時重註冊或移除 trigger。
- 排程執行目標可選 `AGENT`、`TOOL`、`SKILL`，並採統一路由與錯誤分類（`SCHEDULE_VALIDATION/CONFIGURATION/TIMEOUT/RUNTIME/INTERNAL`）。
- 提供 `ISOLATED/SHARED` 上下文模式；`SHARED` 可讀取前次排程摘要與工具結果，並落地 `contextRetentionRuns`（保留最近 N 筆）與 `contextMaxTokens`（超限摘要壓縮）。
- `run-now` 共享上下文採 lazy-load：僅在執行啟動時自資料庫取回，並以 `contextSegmentKey=targetType:targetRefId` 做任務/上下文段映射。
- 新增 Trigger Abstraction：任務可由 `SCHEDULE` 與 `WEBHOOK` 觸發，並共用同一執行、審計與觀測流程。
- 已新增排程觀測快照（`GET /schedules/observability`）：`triggered/completed/failed`、平均耗時、錯誤分類與 targetType 分佈。
- 單向原則：不提供「執行完成後直接回呼舊系統業務端點」作為主流程；若需對外通知，改由通知策略（T21）獨立通道處理。
- 依賴前置：T18/T19 需先完成核心能力與測試驗證，才進入 T20 主流程。

#### T21（通知編排）
- MVP 先支援通知通道 `Email`、`Webhook`；社群通道（Slack/Discord/LINE/Teams）後續擴充。
- 排程或 webhook trigger 在建立時可指定通知策略（開始/成功/失敗/超時）。
- 派送引擎需具備重試、去重、DLQ 與觀測能力。
- 當前進度：`T21-A1~T21-A5` 已完成（資料模型、通道/策略 API、派送引擎、重試退避、回歸測試）。
- **階段建議：納入 Phase C，並接在 T20 基礎能力後實作**，可直接共用 trigger/run 事件來源。
- 依賴前置：T20 基礎流程與測試需先通過，才啟動 T21-A。

### Phase D：後續擴展（Future）
- [ ] 長期記憶（pgvector）與檢索策略
- [ ] 前端任務關係可視化、子代理追蹤與上下文管理儀表板
- [ ] 生產級安全策略（沙盒權限、審計、租戶隔離）

## 里程碑
- Milestone 1（已達成）：單一 Master Agent 可用、具工具調用與狀態推播
- Milestone 2（已達成）：具動態工具、動態模型、最小 Sub Agent 與統一 Agent 介面
- Milestone 3（已達成）：T18 主線完成（agent-utils 全量能力納入，不含 A2A）
- Milestone 4（規劃中）：T18 完成後再啟動 T15（agent-client）整合
