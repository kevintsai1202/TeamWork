# 專案 RoadMap：Team Work 多 Agent 協作系統（2026-02-21 同步版）

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
- [ ] Master 依 sub-agent 描述自主路由（T12）

### Phase C：下一步整合（Next）
- [ ] 串接 `agent-sandbox` 作為可控執行沙盒（T14）
- [ ] 串接 `agent-client` 作為外部 Agent 呼叫通道（T15）
- [ ] 對照 `spring-ai-agent-utils/examples` 補齊缺口能力（T16）

### Phase D：後續擴展（Future）
- [ ] 長期記憶（pgvector）與檢索策略
- [ ] 前端任務關係可視化與子代理追蹤
- [ ] 生產級安全策略（沙盒權限、審計、租戶隔離）

## 里程碑
- Milestone 1（已達成）：單一 Master Agent 可用、具工具調用與狀態推播
- Milestone 2（已達成）：具動態工具、動態模型、最小 Sub Agent 與統一 Agent 介面
- Milestone 3（進行中）：完成自主路由 + sandbox + 外部 agent client 整合
