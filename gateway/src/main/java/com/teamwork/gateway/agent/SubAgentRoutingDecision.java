package com.teamwork.gateway.agent;

/**
 * Sub-agent 路由決策結果。
 */
public record SubAgentRoutingDecision(
        SubAgentDescriptor selected,
        double keywordScore,
        double semanticScore,
        double finalScore,
        boolean fallbackUsed,
        String reason) {
}
