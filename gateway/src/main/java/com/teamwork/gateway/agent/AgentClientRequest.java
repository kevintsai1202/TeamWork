package com.teamwork.gateway.agent;

/**
 * Agent Client 傳輸請求物件。
 */
public record AgentClientRequest(
        String taskId,
        String modelName,
        String provider,
        String inputPayload,
        String selectedSubAgentName,
        String selectedSubAgentReferencePath,
        boolean sandboxEnabled,
        String sandboxType) {
}
