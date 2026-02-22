package com.teamwork.gateway.dto;

import java.util.List;

public record AgentContextDetailResponse(
        String taskId,
        String agentName,
        String systemPrompt,
        List<ContextMessageItem> messages,
        List<CompressedSummaryItem> compressedSummaries,
        List<ToolCallSummaryItem> toolCalls
) {
}
