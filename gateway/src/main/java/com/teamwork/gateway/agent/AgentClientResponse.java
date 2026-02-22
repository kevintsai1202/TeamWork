package com.teamwork.gateway.agent;

/**
 * Agent Client 傳輸回應物件。
 */
public record AgentClientResponse(
        String content,
        String remoteTaskId,
        String rawResponse) {
}
