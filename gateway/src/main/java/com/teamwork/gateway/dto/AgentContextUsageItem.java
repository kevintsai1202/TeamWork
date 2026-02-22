package com.teamwork.gateway.dto;

import java.time.LocalDateTime;

public record AgentContextUsageItem(
        String agentName,
        String taskId,
        long messageCount,
        long estimatedTokens,
        long compressionCount,
        LocalDateTime lastUpdatedAt
) {
}
