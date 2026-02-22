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
- [x] T12：實作 Master 依 sub-agent 描述自主路由（自動決定呼叫哪個 agent）並補齊測試【已完成】
- [x] T13：建立統一 Agent Provider 介面（一般 Agent / Claude SDK 同接口呼叫）並完成整合【已完成】
- [x] T14：串接 `agent-sandbox` 作為可控執行沙盒，並接入統一 Agent Provider 流程【已完成】
- [ ] T15：串接 `agent-client` 作為外部 Agent 呼叫通道（拆分為非CLI先行 + CLI後置）【進行中】
- [ ] T16：比對 `spring-ai-agent-utils/examples`，補齊現有缺口能力（僅缺口，不重做已完成功能）【併入T18】
- [x] T18：`spring-ai-agent-utils` 全量能力納入主線（不含 A2A）【已完成】
- [ ] T19：上下文治理與壓縮（UI 可視化 + 自動壓縮 + 手動刪除）【進行中】
- [x] T20：排程治理（可自由調整 + 多排程 + 指定 Agent/Tool/Skill + 共用上下文）【已完成】
- [x] T21：通知編排（Email/社群通知 + 排程可選通知策略）【已完成（MVP：EMAIL/WEBHOOK）】
- [x] T17：依指定基線重整 `docs/RoadMap.md`，並同步 `docs/development` 文件（spec/api/todolist）【已完成】

## 下一階段差距拆分（發布基線）

### 執行順序與品質門檻（強制）
- [ ] 先完成 T18 與 T19（子任務完成後需測試通過），再進入 T20/T21，最後再處理 T15。
- [ ] 每個子任務完成後，必須執行對應測試並通過，才可進入下一子任務。

### T18/T19 並行泳道（可同時開發）
- [x] Lane A（完成）：T18-4（路由整併為單一路由真相）
- [x] Lane B（完成）：T18-7（觀測面 metrics/log/trace 與錯誤分類）
- [x] Lane C（完成）：T19-1 + T19-3（上下文用量統計 + 完整上下文檢視 API）
- [x] Gate G1：Lane A/B/C 全部完成且測試通過，已可進入 T19-2。
- [x] Gate G2：T19-2、T19-4 完成且測試通過，已可進入 T20 主流程。

### T12：Master 依 sub-agent 描述自主路由
- [x] T12-1：建立 `SubAgentDescriptorRepository`，集中管理 name/description/tools/owner-provider。
- [x] T12-2：新增 `SubAgentRouter`，依任務語意與 descriptor 打分決策（至少含 fallback 規則）。
- [x] T12-3：`MasterAgent` 改為「先路由、後執行」，並保留無匹配時的 `default` 路徑。
- [x] T12-4：補齊路由測試（命中/未命中/多候選平手）與回歸測試。

### T14：串接 agent-sandbox（MVP）
- [x] T14-1：建立 `SandboxExecutionProvider`（adapter）並接 `agent-sandbox`，先打通最小執行鏈路。
- [x] T14-2：在 `UnifiedAgentProvider` 流程中加入 sandbox capability 檢查與 timeout/cancel 控制（MVP 僅需單層策略）。
- [x] T14-3：新增安全保護（語言白名單、最大執行時間、輸出大小上限）並定義標準錯誤碼。
- [x] T14-4：補齊整合測試（成功執行、超時、拒絕語言、執行失敗回報）。
- [x] T14-5：延後項盤點（多層 fallback、細粒度策略引擎、完整審計）並移入後續版本。
- [x] T14-6：加入 DockerSandbox（agent-sandbox-docker）支援，AgentExecutionContext 新增 sandboxType/sandboxDockerImage，SandboxExecutionProvider 依 sandboxType 路由（LOCAL/DOCKER），並加入 @Tag("docker") 整合測試。
- [x] T14-7：加入 E2BSandbox（agent-sandbox-e2b）支援，SandboxExecutionProvider 解開 E2B switch case（apiKey 優先 yml > 環境變數，template 複用 dockerImage 欄位），application.yml 新增 sandbox.e2b.api-key 設定。

### T15：串接 agent-client
- 狀態：拆分執行
- T15-A（非 CLI，相依低，可先做）
	- [ ] T15-A1：新增 `AgentClientUnifiedAgentProvider` 骨架與 transport 抽象。
	- [ ] T15-A2：補齊 provider 選擇策略（依 `ai_models.provider` + endpoint 配置）。
	- [ ] T15-A3：加入重試與錯誤分類（連線錯誤/4xx/5xx）及可觀測性欄位。
	- [ ] T15-A4：補齊契約測試與降級策略（外部失敗時 fallback）。
- T15-B（CLI 相依，後置）
	- [ ] T15-B1：CLI transport adapter 落地（命令、參數、錯誤碼映射）。
	- [ ] T15-B2：雲主機環境安裝/權限/執行腳本與運維手冊。

### T16：比對 examples 補缺口
- [ ] 併入 T18，不獨立執行（原 T16 子任務改由 T18 能力矩陣統一管理）。

### T18：Agent Utils 全量能力納入（不含 A2A）
- [x] T18-1：建立能力矩陣（capability matrix），定義現況/缺口/優先級。
- [x] T18-2：抽象工具調用層（tool abstraction），統一 callback/provider 接點。
- [x] T18-3：導入 skills 熱更新流程（runtime refresh + cache invalidation）。
- [x] T18-4：路由整併為單一路由真相（single routing source of truth）。
- [x] T18-5：納入 ask-user-question 能力與回問治理規則。
- [x] T18-6：補齊 RBAC + sandbox 安全治理策略。
- [x] T18-7：建立觀測面（metrics/log/trace）與錯誤分類。
- [x] T18-8：完成相容層（compatibility layer）與回歸驗證。

### T19：上下文治理與壓縮（關鍵功能）
- [x] T19-1：建立上下文用量統計（每個 agent/task 的 messageCount、estimatedTokens、compressionCount）。
- [x] T19-2：實作自動壓縮流程（預設啟用，支援 threshold/target/retainRecentMessages 設定）。
- [x] T19-3：提供完整上下文檢視 API（原始訊息/壓縮摘要/工具調用摘要）。
- [x] T19-4：提供手動刪除上下文 API（單筆/區間/摘要/全歷史）與審計紀錄。
- [ ] T19-5：前端新增 Context 管理頁（可視化每個 agent 用量、檢視內容、手動刪除）。
- [x] T19-6：觀測性補齊（context.compressed 事件、壓縮前後 token 指標、失敗率）。
- [x] T19-7：補齊壓縮提示詞模板與回歸測試（確保關鍵資訊不遺失）。

### T20：排程治理與共用上下文（關鍵功能）
- [x] T20-0：抽象任務觸發層（Trigger Abstraction），統一支援 `SCHEDULE` 與 `WEBHOOK` 來源。
- [x] T20-1：建立排程資料模型（`task_schedules`、`schedule_runs`、`schedule_context_snapshots`）與 migration（現階段採 JPA `ddl-auto=update` schema 演進）。
- [x] T20-2：實作排程管理 API（建立/更新/啟用/停用/立即執行/刪除/查詢）【已完成】。
- [x] T20-3：實作多排程執行器（CRON/INTERVAL）與動態重載 trigger。
- [x] T20-4：實作執行目標路由（`targetType=AGENT|TOOL|SKILL`）與統一錯誤分類。
- [x] T20-5：實作共用上下文模式（`contextMode=ISOLATED|SHARED`）與快照讀寫。
- [x] T20-6：實作上下文保留策略（`contextRetentionRuns`）與 token 壓縮策略（`contextMaxTokens`）。
- [x] T20-7：補齊觀測性與審計（schedule.triggered/completed/failed、成功率、耗時、重試次數）。
- [x] T20-8：補齊回歸測試（多排程並行、共享上下文續跑、更新排程即時生效）。

### T21：通知編排（關鍵功能）
- [x] T21-A1（MVP）：建立通知資料模型（`notification_channels`、`notification_policies`、`notification_deliveries`）與 migration。
- [x] T21-A2（MVP）：實作通知通道管理 API（建立/更新/啟用/停用/查詢），先支援 `EMAIL`、`WEBHOOK`。
- [x] T21-A3（MVP）：實作通知策略 API（事件時機、模板、通道綁定）並可掛載到 schedule/trigger。
- [x] T21-A4（MVP）：實作派送引擎（重試、退避、去重、DLQ）與觀測指標，先覆蓋 `EMAIL`、`WEBHOOK`。
- [x] T21-A5（MVP）：補齊回歸測試（成功/失敗通知、重試、策略切換）。
- [ ] T21-B1（擴充）：新增社群通道 `SLACK`、`DISCORD`、`LINE`、`TEAMS` adapter。
- [ ] T21-B2（擴充）：補齊社群通道格式模板與通道相容性測試。

> 依賴關係：T20/T21 開發前，需先完成 T18-4、T18-7、T18-8 與 T19-1~T19-7（除前端 T19-5）的主流程與測試。【已滿足】

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
- 2026-02-22：完成 T12，新增 SubAgentDescriptorRepository + Hybrid Router（AI 語意 + 關鍵字雙重比較），並導入可調權重/門檻（aiWeight、keywordWeight、threshold）與 fallback；全量測試通過且 JaCoCo 達標（LINE 81.19%、BRANCH 62.86%）。
- 2026-02-22：確認 T12 目前先採檔案來源（`agents/subagents/*.md`）管理 sub-agent 描述；DB 化需求保留到後續階段（方便即時編輯與共用）。
- 2026-02-22：啟動 T14 規劃，將範圍收斂為 MVP（最小沙盒接入、timeout、語言白名單、標準化錯誤回傳），並同步 `RoadMap/spec/api/todolist`。
- 2026-02-22：完成 T14，新增 `SandboxExecutionProvider`（含 protected createLocalSandbox() 可測覆寫）、3 個沙盒例外類別（UnsupportedLanguageException/SandboxTimeoutException/SandboxExecutionFailedException）、ErrorResponse 重構（code/message/traceId/timestamp）、GlobalExceptionHandler 沙盒例外映射；AgentProfile 新增 sandboxEnabled/sandboxType，UnifiedAgentRegistry 加入 sandbox routing，MasterAgent 串接 AgentProfileRepository；全量測試 82/82 通過，JaCoCo LINE 84%、BRANCH 62% 達標。
- 2026-02-22：完成 T14-6，加入 DockerSandbox 支援（agent-sandbox-docker:0.9.0-SNAPSHOT）；AgentExecutionContext 擴充為 14 params（+sandboxType/sandboxDockerImage）；SandboxExecutionProvider 改為 @Autowired 3-param 建構子、createSandbox() switch LOCAL/DOCKER；補充 SandboxDockerIntegrationTest（@Tag("docker") 6 tests）；全量測試 91/91 通過。
- 2026-02-22：完成 T14-7，加入 E2BSandbox 支援（agent-sandbox-e2b:0.9.0-SNAPSHOT 自動從 snapshot repo 下載）；SandboxExecutionProvider 加入 e2bApiKey 欄位（application.yml sandbox.e2b.api-key > E2B_API_KEY 環境變數）、createSandbox() 解開 E2B case（template 複用 AgentProfile.dockerImage 欄位）；主程式與測試程式編譯全數通過。
- 2026-02-22：啟動 T18，定義主線範圍為 `spring-ai-agent-utils` 全量能力納入（不含 A2A）。
- 2026-02-22：T15 調整為延後，原因為 CLI 模式在雲主機環境落地與維運成本較高。
- 2026-02-22：新增 T19（上下文治理與壓縮）需求，文件已納入：每個 agent 上下文用量可視化、完整上下文檢視、手動刪除、自動壓縮與閾值設定。
- 2026-02-22：同步 T18 實作進度：已完成 T18-2/T18-3/T18-5/T18-6；待完成 T18-1/T18-4/T18-7/T18-8。
- 2026-02-22：完成 T18-1（能力矩陣文件：`docs/development/T18_capability_matrix.md`）；完成 T18-4 設計稿（`docs/development/T18_routing_unification_design.md`），待進入程式實作與測試後再勾選 T18-4。
- 2026-02-22：調整 T15 執行策略：拆分為 T15-A（非CLI可先做）與 T15-B（CLI相依後置），避免整體進度受 CLI 限制阻塞。
- 2026-02-22：新增 T20（排程治理）需求，納入可自由調整、多排程、指定 Agent/Tool/Skill 與排程共用上下文（可讀取前次排程摘要）。
- 2026-02-22：T20 進入進行中，新增 Trigger Abstraction，統一以排程與 Webhook 觸發任務。
- 2026-02-22：新增 T21（通知編排）需求，排程與 webhook 觸發可選通知策略（Email/社群）；開發階段建議納入 Phase C 並與 T20 串接。
- 2026-02-22：T21 調整為分段交付：MVP 先支援 `EMAIL` + `WEBHOOK`，社群通道（Slack/Discord/LINE/Teams）列入 T21-B 擴充。
- 2026-02-22：優先序調整：先執行 T18/T19，再執行 T20/T21；每個子任務都必須測試通過才可前進。
- 2026-02-22：完成 T20-1（JPA 實體/Repository）：新增 `TaskSchedule`、`ScheduleRun`、`ScheduleContextSnapshot`、`TaskTrigger` 與對應 repository，並補齊 `EntityCoverageTest`。
- 2026-02-23：導入 T18/T19 並行泳道（Lane A/B/C）與 Gate G1/G2，允許並行開發但維持測試閘控。
- 2026-02-23：正式啟動三個並行處理（Lane A/B/C），進入同步開發；各 Lane 完成後需各自測試通過再進入 Gate G1。
- 2026-02-23：完成 Lane A（T18-4）驗收：新增 `AgentRoutingServiceTest`，並通過相關測試與全量測試（151/151）。
- 2026-02-23：完成 Lane B（T18-7）驗收：新增 `AgentObservabilityService`/`AgentObservabilitySnapshot` 與對應測試，並通過全量測試（154/154）。
- 2026-02-23：完成 Lane C（T19-1 + T19-3）：新增 `ContextQueryService`、`ContextController` 與對應 DTO/測試（`ContextQueryServiceTest`、`ContextControllerTest`），全量測試通過（162/162），Gate G1 達成。
- 2026-02-23：完成 T19-2（自動壓縮）：新增 `ContextCompressionService` 並接入 `RedisChatMemory` 寫入流程，支援 `threshold/target/retainRecentMessages` 設定與 `compressionCount` 統計；全量測試通過（166/166）。
- 2026-02-23：完成 T19-4（手動刪除 + 審計）：新增 `DELETE /agents/context/{taskId}`，支援 `SINGLE_MESSAGE/RANGE/SUMMARY/ALL_HISTORY`，並新增 `context_deletion_audits` 審計實體；全量測試通過（173/173），Gate G2 達成。
- 2026-02-23：完成 T19-6（觀測）與 T19-7（壓縮模板）：新增 `ContextCompressedEvent` + SSE `context.compressed` 事件派送、`ContextCompressionMetricsSnapshot`（attempts/compressed/failures/failureRate/averageSavedRatio）；`ContextCompressionService` 導入 Stage A/Stage B 結構化壓縮摘要（含 1~5 段）；相關測試通過（14/14）。
- 2026-02-23：完成 T18-8（相容層 + 回歸基線）：新增 `AgentProviderCompatibilityLayer`（legacy provider alias 正規化），`UnifiedAgentRegistry` 接入相容層後再做 provider resolve；新增 `AgentProviderCompatibilityLayerTest` 並擴充 `UnifiedAgentRegistryTest` 驗證 legacy alias（如 `agent-client`/`claude-sdk`）可正常路由；相關測試通過（10/10）。
- 2026-02-23：完成 T20-2（排程管理 API）：新增 `ScheduleController` 與 `ScheduleService`，提供 `/schedules` 建立/更新/啟停/刪除/查詢、`/schedules/{scheduleId}/run-now`、`/schedules/{scheduleId}/runs`；`run-now` 在啟動時依 `scheduleId + contextSegmentKey`（`targetType:targetRefId`）自 DB lazy-load SHARED 上下文，執行完成後回寫 `schedule_context_snapshots`，並補齊單元測試。
- 2026-02-23：補齊舊系統單向事件入口（Webhook Trigger）：新增 `TriggerController`（`POST /triggers/webhooks/{webhookKey}`）與 `TriggerExecutionService`，接收事件後建立 `TaskRecord(PENDING)` 並非同步交由 `MasterAgent` 執行；完成通知沿用既有 `TaskStatusChangeEvent/SSE`，新增 `TriggerExecutionServiceTest`、`TriggerControllerTest` 並通過測試。
- 2026-02-23：完成 webhook 安全強化（簽章 + 重放防護）：新增 `WebhookSecurityService`，實作 `HmacSHA256` canonical string 驗證、`X-Trigger-Timestamp` 時間窗（300s）、`X-Trigger-Nonce` TTL 去重；`TriggerController/TriggerExecutionService` 串接安全 headers 驗證，新增 `WebhookSecurityServiceTest`，並通過相關測試（11/11）。
- 2026-02-23：完成 T20-3（多排程執行器 + 動態重載）：新增 `ScheduleRuntimeExecutor` 與 `ScheduleTriggeredEvent`，啟動時自動重載啟用排程，並在 `ScheduleService` 的建立/更新/啟用/停用/刪除時即時重載或移除 trigger；新增 `ScheduleRuntimeConfig` 提供專用 `TaskScheduler`；測試 `ScheduleServiceTest` + `ScheduleRuntimeExecutorTest` 通過（10/10）。
- 2026-02-23：完成 T20-4/T20-5/T20-6/T20-7：新增 `ScheduleTargetDispatchService`（`AGENT/TOOL/SKILL` 分派）、`ScheduleObservabilityService`（triggered/completed/failed 與錯誤分類/目標分佈快照）；`ScheduleService` 串接統一錯誤碼、共享上下文 retention/compression policy；新增 `GET /schedules/observability` API 與對應測試。
- 2026-02-23：完成 T20-8 回歸驗證：排程相關目標測試全數通過（18/18），全量測試通過（210/210）；T20 全子任務（T20-0~T20-8）完成。
- 2026-02-23：啟動 T21-A 文件前置：已同步 `RoadMap/spec/api/todolist` 的通知編排範圍與任務拆分，下一步進入 `T21-A1`（通知資料模型）實作。
- 2026-02-23：完成 T21（A1~A5）：新增通知資料模型與 API、派送引擎（去重/重試退避/DLQ）、排程與 webhook trigger 串接、SSE `notification.sent/notification.failed`。修正測試阻塞根因（缺少 `ObjectMapper` Bean）後新增 `JacksonConfig`，全量測試通過（240/240）。
