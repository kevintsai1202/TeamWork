# TeamWork API 規格（api）

Base URL：`/api/v1`

## 共通規範
- Content-Type：`application/json`
- 認證：`Authorization: Bearer <token>`
- 多租戶：由 Token 解析 `tenant_id`，不可由前端任意指定
- 時間格式：ISO-8601（UTC）

## 版本同步註記（2026-02-23）
- T18：作為主線進行中，目標為納入 `spring-ai-agent-utils` 全量能力（不含 A2A）。
- T19：已完成 `T19-1`（用量統計）、`T19-2`（自動壓縮）、`T19-3`（完整檢視）、`T19-4`（手動刪除與審計）、`T19-6`（context.compressed 事件與壓縮觀測指標）、`T19-7`（壓縮模板與回歸測試）。
- T20：已完成 `T20-1`（排程資料模型）、`T20-2`（排程管理 API）、`T20-3`（多排程執行器）、`T20-4`（目標路由+錯誤分類）、`T20-5`（共享上下文快照）、`T20-6`（保留與壓縮策略）、`T20-7`（排程觀測快照）、`T20-8`（回歸測試與驗證）。
- T15：改為分段執行（T15-A 非CLI先行；T15-B CLI相依後置）。
- T16：併入 T18，不再獨立執行，相關能力缺口改由 T18 統一追蹤與交付。
- T21：已完成 MVP（`EMAIL`、`WEBHOOK`）：通知通道/策略 API、派送紀錄查詢、排程與 webhook trigger 通知派送、SSE 通知事件。

### 身分與使用者邊界
- 身分驗證（登入、憑證簽發、MFA）由外部 IdP/認證系統負責。
- Gateway 內 `users` 為投影資料（`id/tenant_id/username/display_name/status`），只用於授權與任務建立驗證。
- Gateway 不提供密碼管理 API，不保存密碼或 refresh token。

## 錯誤格式
```json
{
  "code": "TASK_NOT_FOUND",
  "message": "Task not found",
  "traceId": "01H...",
  "timestamp": "2026-02-21T10:00:00Z"
}
```

---

## 1. Task APIs

### 1.1 建立任務
- Method：`POST /tasks`
- 說明：建立主任務或子任務
- 必填欄位：`userId`、`profileId`、`inputPayload`
- 驗證行為：若任一必填缺漏或空字串，回傳 `400 Bad Request`
- 使用者驗證：`userId` 必須存在於 Gateway `users`，且 `status=ACTIVE`
- 測試階段（dev/test）：允許以 request body 的 `userId` 指定測試使用者；正式環境改由 Token claims 提供
- 測試預設種子（dev/test）：`u_alice`、`u_bob`

Request
```json
{
  "userId": "usr_001",
  "profileId": "apf_001",
  "inputPayload": "請分析這份需求並提出實作方案",
  "parentTaskId": null,
  "metadata": {
    "source": "web"
  }
}
```

Response `202 Accepted`
```json
{
  "taskId": "tsk_001",
  "status": "PENDING",
  "createdAt": "2026-02-21T10:00:00Z"
}
```

Response `400 Bad Request`（欄位驗證失敗）
```json
{
  "status": 400,
  "message": "userId is required",
  "timestamp": "2026-02-22T09:00:00Z"
}
```

### 1.2 查詢任務
- Method：`GET /tasks/{taskId}`

Response `200 OK`
```json
{
  "taskId": "tsk_001",
  "parentTaskId": null,
  "status": "RUNNING",
  "profileId": "apf_001",
  "input": "...",
  "createdAt": "2026-02-21T10:00:00Z",
  "updatedAt": "2026-02-21T10:00:10Z"
}
```

### 1.3 取消任務
- Method：`POST /tasks/{taskId}:cancel`

Response `202 Accepted`
```json
{
  "taskId": "tsk_001",
  "status": "CANCELLED"
}
```

### 1.4 查詢任務輸出
- Method：`GET /tasks/{taskId}/outputs`

Response `200 OK`
```json
{
  "taskId": "tsk_001",
  "outputs": [
    {
      "outputId": "out_001",
      "type": "MARKDOWN",
      "mimeType": "text/markdown",
      "content": "# 分析結果...",
      "artifactUrl": null,
      "createdAt": "2026-02-21T10:01:30Z"
    }
  ]
}
```

---

## 2. 即時推播 APIs

### 2.1 SSE 訂閱
- Method：`GET /stream`
- Query：`taskId`（可選，不帶表示訂閱本人可見任務）
- Header：`Accept: text/event-stream`

Event 範例
```text
event: task.status.changed
data: {"taskId":"tsk_001","status":"RUNNING","updatedAt":"2026-02-21T10:00:10Z"}

event: task.completed
data: {"taskId":"tsk_001","status":"COMPLETED","summary":"任務完成"}
```

---

## 3. Model Registry APIs

### 3.1 建立模型
- Method：`POST /models`

Request
```json
{
  "name": "gpt-4o",
  "provider": "openai",
  "endpoint": "https://api.openai.com/v1",
  "apiKey": "sk-***",
  "status": "ACTIVE"
}
```

### 3.2 查詢模型清單
- Method：`GET /models`

---

## 4. Tool Registry 與 RBAC APIs

### 4.1 建立工具
- Method：`POST /tools`

Request
```json
{
  "name": "firecrawl_search",
  "type": "MCP",
  "configuration": {
    "server": "firecrawl"
  },
  "status": "ACTIVE"
}
```

### 4.2 設定使用者工具權限
- Method：`PUT /users/{userId}/tool-permissions`

Request
```json
{
  "toolIds": ["tool_001", "tool_003"]
}
```

---

## 5. Agent Profile APIs

### 5.1 建立 Agent Profile
- Method：`POST /agent-profiles`

Request
```json
{
  "name": "Researcher",
  "systemPrompt": "你是研究型代理人",
  "defaultModelId": "mdl_001",
  "toolIds": ["tool_001", "tool_003"]
}
```

### 5.2 查詢 Agent Profile
- Method：`GET /agent-profiles/{profileId}`

---

## 6. 管理與健康檢查

### 6.1 健康檢查
- Method：`GET /health`
- Response：`200 OK`

### 6.2 事件延遲監控
- Method：`GET /admin/streams/lag`
- 說明：回報 Redis Streams consumer lag（管理員限定）

---

## 7. 上下文治理 APIs（T19）

### 7.1 查詢 Agent 上下文用量
- Method：`GET /agents/context/usage`
- Query：`agentName`（可選）、`taskId`（可選）、`status`（可選）

Response `200 OK`
```json
{
  "items": [
    {
      "agentName": "master-agent",
      "taskId": "tsk_001",
      "messageCount": 42,
      "estimatedTokens": 10980,
      "compressionCount": 2,
      "lastUpdatedAt": "2026-02-22T13:20:11Z"
    }
  ]
}
```

### 7.2 取得完整上下文內容
- Method：`GET /agents/context/{taskId}`
- 說明：回傳原始訊息、壓縮摘要、工具呼叫摘要

Response `200 OK`
```json
{
  "taskId": "tsk_001",
  "agentName": "master-agent",
  "systemPrompt": "你是...",
  "messages": [
    {
      "index": 1,
      "role": "user",
      "content": "...",
      "estimatedTokens": 320
    }
  ],
  "compressedSummaries": [
    {
      "summaryId": "sum_001",
      "content": "1. 目標與範圍...",
      "sourceRange": "1-30",
      "createdAt": "2026-02-22T13:00:00Z"
    }
  ],
  "toolCalls": [
    {
      "toolName": "askUserQuestion",
      "arguments": "{...}",
      "resultPreview": "Question sent..."
    }
  ]
}
```

### 7.3 手動刪除上下文
- Method：`DELETE /agents/context/{taskId}`

Request
```json
{
  "mode": "RANGE",
  "range": {
    "fromIndex": 5,
    "toIndex": 20
  },
  "reason": "remove irrelevant logs"
}
```

`mode` 可選值：
- `SINGLE_MESSAGE`
- `RANGE`
- `SUMMARY`
- `ALL_HISTORY`

Response `200 OK`
```json
{
  "taskId": "tsk_001",
  "removed": true,
  "removedCount": 16,
  "auditId": "audit_ctx_001"
}
```

### 7.4 查詢/更新自動壓縮策略

> 實作同步註記（2026-02-23）：
> - 後端已落地自動壓縮流程（`threshold/target/retainRecentMessages`），目前由 `application.yml` 管理策略。
> - `GET/PUT /admin/context-compression/policy` 仍屬後續管理面 API，將於後續子任務補齊。

#### 7.4.1 查詢策略
- Method：`GET /admin/context-compression/policy`

Response `200 OK`
```json
{
  "enabled": true,
  "thresholdTokens": 12000,
  "targetTokens": 6000,
  "retainRecentMessages": 8
}
```

#### 7.4.2 更新策略
- Method：`PUT /admin/context-compression/policy`

Request
```json
{
  "enabled": true,
  "thresholdTokens": 14000,
  "targetTokens": 7000,
  "retainRecentMessages": 10
}
```

Response `200 OK`
```json
{
  "updated": true
}
```

### 7.5 壓縮審計事件（SSE）
- Event：`context.compressed`

Event data 範例
```text
event: context.compressed
data: {"taskId":"tsk_001","agentName":"master-agent","beforeTokens":15820,"afterTokens":6120,"savedTokens":9700,"savedRatio":0.613}
```

---

## 8. 排程治理 APIs（T20）

> 觸發抽象：排程與 webhook 皆透過同一 Trigger 執行鏈路建立任務，僅 `triggerSource` 不同。

### 8.1 建立排程
- Method：`POST /schedules`

Request
```json
{
  "name": "每日研究摘要",
  "enabled": true,
  "scheduleType": "CRON",
  "cronExpr": "0 9 * * *",
  "timezone": "Asia/Taipei",
  "targetType": "AGENT",
  "targetRefId": "apf_researcher_001",
  "payload": {
    "input": "請彙整昨日任務結果並產出重點"
  },
  "contextMode": "SHARED",
  "contextRetentionRuns": 20,
  "contextMaxTokens": 8000,
  "notificationPolicyId": "np_001",
  "maxConcurrentRuns": 1,
  "priority": 5
}
```

Response `201 Created`
```json
{
  "scheduleId": "sch_001",
  "nextRunAt": "2026-02-23T01:00:00Z"
}
```

### 8.2 查詢排程清單
- Method：`GET /schedules`
- Query：`tenantId`（必填）、`enabled`（可選）、`targetType`（可選）

### 8.3 更新排程
- Method：`PUT /schedules/{scheduleId}`
- 說明：可調整 cron/interval/timezone/payload/target/context 設定，更新後即時生效。

### 8.4 啟用/停用排程
- Method：`PATCH /schedules/{scheduleId}/enable`
- Method：`PATCH /schedules/{scheduleId}/disable`

### 8.5 立即執行一次
- Method：`POST /schedules/{scheduleId}/run-now`

> 實作同步註記（2026-02-23）：
> - 當 `contextMode=SHARED`，`run-now` 會在「執行啟動時」依 `scheduleId + contextSegmentKey` 從 `schedule_context_snapshots` lazy-load 最近上下文。
> - `contextSegmentKey` 採 `targetType:targetRefId`（例如 `AGENT:apf_researcher_001`），確保任務與上下文段可精準配對。
> - 執行完成後會回寫新的 `schedule_context_snapshots`。

Response `202 Accepted`
```json
{
  "scheduleId": "sch_001",
  "runId": "run_001",
  "status": "PENDING"
}
```

### 8.6 查詢排程執行紀錄
- Method：`GET /schedules/{scheduleId}/runs`

Response `200 OK`
```json
{
  "items": [
    {
      "runId": "run_001",
      "triggerType": "AUTO",
      "status": "SUCCESS",
      "startedAt": "2026-02-23T01:00:01Z",
      "finishedAt": "2026-02-23T01:00:07Z",
      "durationMs": 6200
    }
  ]
}
```

### 8.9 查詢排程觀測快照（T20-7）
- Method：`GET /schedules/observability`
- 說明：回傳排程觸發/完成/失敗計數、平均耗時、錯誤分類分佈、目標類型分佈

Response `200 OK`
```json
{
  "triggeredCount": 120,
  "completedCount": 110,
  "failedCount": 10,
  "avgDurationMs": 533,
  "failedByCategory": {
    "VALIDATION": 4,
    "RUNTIME": 6
  },
  "targetTypeCounts": {
    "AGENT": 80,
    "TOOL": 25,
    "SKILL": 15
  }
}
```

### 8.7 查詢排程共用上下文快照
- Method：`GET /schedules/{scheduleId}/context`
- Query：`limit`（可選，預設 20）

Response `200 OK`
```json
{
  "scheduleId": "sch_001",
  "contextMode": "SHARED",
  "items": [
    {
      "runId": "run_001",
      "messageCount": 18,
      "estimatedTokens": 3210,
      "contextSummary": "前次已完成資料彙整，待補 API 健康檢查",
      "toolResultSummary": "firecrawl_search 命中 6 筆來源",
      "pendingTodos": ["驗證來源可信度"],
      "createdAt": "2026-02-23T01:00:07Z"
    }
  ]
}
```

### 8.8 清理排程共用上下文
- Method：`DELETE /schedules/{scheduleId}/context`
- Query：`mode=ALL|BEFORE_RUN_ID`

### 8.9 排程事件（SSE）
- Event：`schedule.triggered`
- Event：`schedule.completed`
- Event：`schedule.failed`

Event data 範例
```text
event: schedule.completed
data: {"scheduleId":"sch_001","runId":"run_001","status":"SUCCESS","durationMs":6200}
```

### 8.10 Webhook 觸發任務
- Method：`POST /triggers/webhooks/{webhookKey}`
- Header：`X-Trigger-Timestamp`、`X-Trigger-Nonce`、`X-Trigger-Signature`
- 說明：以 webhook 觸發任務，觸發來源標記為 `WEBHOOK`，並套用簽章驗證與重放保護。
- 簽章演算法：`HmacSHA256`
- 簽章原文（canonical string）：`timestamp + "\n" + nonce + "\n" + webhookKey + "\n" + inputPayload + "\n" + idempotencyKey`
- 時間窗：`X-Trigger-Timestamp` 與伺服器時間差需在 300 秒內。
- 重放防護：`X-Trigger-Nonce` 在 TTL 內僅可使用一次（預設 TTL 300 秒）。
- 單向原則：此 API 僅做事件接收（ingress）與受理回覆；不在同步回應中回傳 Agent 完成結果，也不回呼舊系統業務流程。
- 實作同步註記（2026-02-23）：已落地 `TriggerController/TriggerExecutionService`，目前 webhook 目標先支援 `targetType=AGENT`，並新增簽章與 nonce 重放防護。

Request
```json
{
  "targetType": "SKILL",
  "targetRefId": "skill.daily-report",
  "payload": {
    "input": "請生成今日摘要"
  },
  "contextMode": "SHARED",
  "idempotencyKey": "evt_20260222_001"
}
```

Response `202 Accepted`
```json
{
  "triggerSource": "WEBHOOK",
  "runId": "task_019",
  "status": "PENDING"
}
```

Response `400 Bad Request`（簽章/時間窗/nonce 驗證失敗）
```json
{
  "code": "INVALID_ARGUMENT",
  "message": "Invalid webhook signature",
  "traceId": "...",
  "timestamp": "2026-02-23T10:00:00"
}
```

結果取得方式（非同步）：
- 任務完成時會發送事件通知（既有 `TaskStatusChangeEvent`，可由 SSE `GET /stream?taskId=...` 訂閱）
- 由舊系統主動查詢執行紀錄 API（例如 `GET /schedules/{scheduleId}/runs`）
- 或由通知策略（T21）推送到獨立通知通道（Email/Webhook）

### 8.11 統一觸發執行 API（內部/管理用途）
- Method：`POST /triggers:execute`
- 說明：統一入口，支援 `triggerSource=SCHEDULE|WEBHOOK|MANUAL`，由後端路由到對應目標。

Request
```json
{
  "triggerSource": "SCHEDULE",
  "scheduleId": "sch_001",
  "targetType": "AGENT",
  "targetRefId": "apf_researcher_001",
  "notificationPolicyId": "np_001",
  "payload": {
    "input": "請產出週報"
  }
}
```

---

## 9. 通知編排 APIs（T21）

> T21 分段：MVP 先支援 `EMAIL`、`WEBHOOK`；社群通道（`SLACK`、`DISCORD`、`LINE`、`TEAMS`）為後續擴充。

### 9.1 建立通知通道
- Method：`POST /notifications/channels`

Request
```json
{
  "tenantId": "tenant-a",
  "name": "Ops Mail",
  "channelType": "EMAIL",
  "endpointConfigJson": "{\"to\":[\"ops@example.com\"],\"subjectPrefix\":\"[TeamWork]\"}",
  "enabled": true
}
```

### 9.2 查詢通知通道
- Method：`GET /notifications/channels`

### 9.3 建立通知策略
- Method：`POST /notifications/policies`

Request
```json
{
  "tenantId": "tenant-a",
  "name": "排程失敗告警",
  "onStarted": true,
  "onSuccess": true,
  "onFailed": true,
  "onTimeout": false,
  "channelIds": ["nc_001", "nc_002"],
  "templateId": "tmpl_schedule_default"
}
```

### 9.4 綁定通知策略到排程
- Method：`PUT /notifications/schedules/{scheduleId}/policy`

Request
```json
{
  "notificationPolicyId": "np_001"
}
```

### 9.5 查詢通知派送紀錄
- Method：`GET /notifications/deliveries`
- Query：`runId`（可選）、`tenantId`（可選）

### 9.6 通知事件（SSE）
- Event：`notification.sent`
- Event：`notification.failed`

Event data 範例
```text
event: notification.failed
data: {"runId":"run_001","channelType":"EMAIL","attempt":3,"error":"smtp-timeout"}
```
```

---

## 狀態碼約定
- `200`：成功查詢
- `201`：成功建立
- `202`：已接受非同步任務
- `400`：參數錯誤
- `401`：未授權
- `403`：無權限
- `404`：資源不存在
- `409`：狀態衝突
- `429`：流量限制
- `500`：系統錯誤

## 版本策略
- 採 URI 版號：`/api/v1`
- 破壞性變更升版至 `v2`
- 事件 payload 增欄位遵守向下相容

## 測試基線（Gateway）
- 整合測試使用 `docker-compose` 的 PostgreSQL（`localhost:15432`）。
- 移除 Testcontainers 依賴與 `@Testcontainers`/`@Container`/`@ServiceConnection` 註解。
- 覆蓋率門檻（JaCoCo）：`LINE >= 80%`、`BRANCH >= 60%`。

## 相依版本基線（Gateway）
- Spring Boot：`4.0.x`
- Spring AI：`2.0.0-M2`
- Spring AI Agent Utils：`org.springaicommunity:spring-ai-agent-utils:0.4.2`
- OpenAI 整合採 `spring-ai-openai`（非 starter），避免啟動期綁定固定 API Key

## Agent Tool Calling（Backend Internal）
- `MasterAgent` 支援 Spring AI Tool Calling，工具由後端註冊並受權限策略控管。
- 對話上下文使用 `taskId` 隔離，避免不同任務/使用者互相污染。
- 工具載入採動態模式：任務執行時由 registry 從資料庫與記憶體快取決定本次可用工具集合，支援不重啟切換工具開關。
- 啟動時會自動讀取 `.env` 的 `MODEL`、`BASE_URL`、`API_KEY` 寫入 `ai_models` 作為預設模型。
- Sub Agent（內部流程）：`MasterAgent` 透過 `TaskToolCallbackProvider` 掛載 task/tool callbacks，子代理以 markdown 定義（`agents/subagents/*.md`）與 `SubagentReference` 載入。
- Sub Agent 自主決策（T12）：Master 會以 sub-agent 描述做 AI 語意比對，並與關鍵字規則分數融合；權重與門檻可由設定檔調整（預設 `ai=0.6`、`keyword=0.4`、`threshold=0.55`）。
- 統一呼叫介面（內部流程）：`MasterAgent` 僅透過 `UnifiedAgentProvider` 執行 agent；不同來源（Spring AI、Claude SDK、未來 Copilot SDK）以 provider 形式接入，不改動上層呼叫契約。
- 後續整合來源（固定基線）：
  - Sandbox 使用 `agent-sandbox`
  - 外部 Agent Client 使用 `agent-client`
  - Agent 工具調用使用 `spring-ai-agent-utils` 與其 `examples`
  - 已有能力不重做，僅針對缺少的 adapter / sandbox / client 連接點補齊
- T14（Sandbox）MVP 契約（內部流程）：
  - 由 `UnifiedAgentProvider` 進行 capability 檢查，僅在 profile/任務標記需沙盒時走 sandbox provider。
  - 請求最小欄位：`language`、`sourceCode`、`timeoutMs`（超過上限時強制裁切到系統上限）。
  - 安全限制：僅允許語言白名單，違反時回傳 `400`（`UNSUPPORTED_LANGUAGE`）。
  - 超時回應：回傳 `408`（`SANDBOX_TIMEOUT`）；執行錯誤回傳 `422`（`SANDBOX_EXECUTION_FAILED`）。
  - 回傳格式統一包含：`taskId`、`provider`、`exitCode`、`stdout`、`stderr`、`durationMs`。
- 文件同步狀態：已於 2026-02-23 與 `docs/RoadMap.md`、`docs/development/spec.md`、`docs/development/todolist.md` 對齊（T19-6/T19-7 已完成並同步註記）
