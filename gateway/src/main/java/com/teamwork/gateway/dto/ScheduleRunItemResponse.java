package com.teamwork.gateway.dto;

import java.time.LocalDateTime;

public record ScheduleRunItemResponse(
        String runId,
        String triggerType,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Long durationMs
) {
}
