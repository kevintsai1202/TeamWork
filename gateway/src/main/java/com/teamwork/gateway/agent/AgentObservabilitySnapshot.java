package com.teamwork.gateway.agent;

import java.util.Map;

/**
 * Agent 觀測快照資料。
 */
public record AgentObservabilitySnapshot(
        long started,
        long completed,
        long failed,
        long averageDurationMs,
        Map<String, Long> failedByCategory) {
}
