package com.teamwork.gateway.agent;

/**
 * 路由決策結果封裝：提供單一入口輸出的執行上下文與 provider。
 */
public record RoutingPlan(
        AgentExecutionContext executionContext,
        UnifiedAgentProvider provider,
        SubAgentRoutingDecision subAgentRoutingDecision) {
}
