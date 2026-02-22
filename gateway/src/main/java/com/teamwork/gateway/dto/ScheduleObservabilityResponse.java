package com.teamwork.gateway.dto;

import java.util.Map;

/**
 * 排程觀測快照回應。
 */
public record ScheduleObservabilityResponse(
        long triggeredCount,
        long completedCount,
        long failedCount,
        long avgDurationMs,
        Map<String, Long> failedByCategory,
        Map<String, Long> targetTypeCounts) {
}
