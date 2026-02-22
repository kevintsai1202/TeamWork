# T18 Capability Matrix（spring-ai-agent-utils 全量能力盤點，不含 A2A）

更新日期：2026-02-22

## 1) 盤點範圍
- 來源：`spring-ai-agent-utils` 核心能力 + `examples` 常見模式。
- 本矩陣僅涵蓋 TeamWork `gateway` 現行主線，不含 A2A。
- 目的：作為 T18 驗收與缺口追蹤單一依據。

## 2) 狀態定義
- `Done`：已在程式與測試中落地。
- `Partial`：部分落地，尚缺整併/治理/觀測。
- `Gap`：尚未落地。

## 3) 能力矩陣

| # | 能力項 | 狀態 | 對應任務 | 現況說明 | 缺口 | 優先級 |
|---|---|---|---|---|---|---|
| 1 | Tool abstraction（callback/provider 統一） | Done | T18-2 | 已有 `ToolProviderAdapter` + `ToolCapabilityRegistry` + `DynamicToolRegistry` | - | P0 |
| 2 | Dynamic tool loading（runtime） | Done | T18-2 | 任務前動態決定可用工具，含快取 | - | P0 |
| 3 | Skills catalog + refresh | Done | T18-3 | `SkillsCatalogService` 支援快取與 refresh | - | P0 |
| 4 | Ask-user-question（human-in-the-loop） | Done | T18-5 | `askUserQuestion/listPendingUserQuestions/submitUserAnswer` 已落地 | - | P0 |
| 5 | Built-in tool policy governance | Done | T18-6 | `AskUserToolPolicyService` 可控制預設工具可用性 | - | P0 |
| 6 | Sandbox governance baseline | Done | T18-6 | LOCAL/DOCKER/E2B + allowlist/timeout/錯誤碼 | - | P0 |
| 7 | Routing single source of truth | Partial | T18-4 | 目前路由分散於 `MasterAgent`/`SubAgentRouter`/`UnifiedAgentRegistry` | 缺統一決策入口 | P0 |
| 8 | Capability matrix as release artifact | Done | T18-1 | 本文件建立完成 | 後續需持續維護 | P1 |
| 9 | Observability（metrics/log/trace taxonomy） | Gap | T18-7 | 目前以 log/event 為主，無完整 metrics taxonomy | 缺 Micrometer 指標與 dashboard 契約 | P0 |
|10| Compatibility layer + regression baseline | Done | T18-8 | 新增 `AgentProviderCompatibilityLayer` 並接入 `UnifiedAgentRegistry`，完成 legacy provider alias 正規化與回歸測試 | - | P1 |

## 4) 優先推進建議
1. **T18-4（P0）**：先做路由單一真相整併，降低決策分叉風險。
2. **T18-7（P0）**：補齊 metrics/log/trace，讓路由與工具治理可觀測。
3. **T18-8（P1）**：已完成相容層與回歸門檻，作為發布守門基線。

## 5) 驗收準則（T18）
- T18-1：能力矩陣存在且可對應到程式位置與任務狀態（本文件）。
- T18-4：路由決策入口收斂為單一服務，`MasterAgent` 不再分散決策。
- T18-7：可量測工具/agent 路由、錯誤分類、延遲與成功率。
- T18-8：已建立相容層與回歸測試（`AgentProviderCompatibilityLayerTest`、`UnifiedAgentRegistryTest`）並可重複執行驗證。
