# TeamWork API 規格（api）

Base URL：`/api/v1`

## 共通規範
- Content-Type：`application/json`
- 認證：`Authorization: Bearer <token>`
- 多租戶：由 Token 解析 `tenant_id`，不可由前端任意指定
- 時間格式：ISO-8601（UTC）

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

Request
```json
{
  "profileId": "apf_001",
  "input": "請分析這份需求並提出實作方案",
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
