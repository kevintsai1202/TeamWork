# TeamWork API 規格（api）

Base URL：`/api/v1`

## 共通規範
- Content-Type：`application/json`
- 認證：`Authorization: Bearer <token>`
- 多租戶：由 Token 解析 `tenant_id`，不可由前端任意指定
- 時間格式：ISO-8601（UTC）

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
- 文件同步狀態：已於 2026-02-21 與 `docs/RoadMap.md`、`docs/development/spec.md`、`docs/development/todolist.md` 對齊
