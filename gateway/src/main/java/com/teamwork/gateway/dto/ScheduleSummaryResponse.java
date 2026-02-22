package com.teamwork.gateway.dto;

import java.time.LocalDateTime;

public record ScheduleSummaryResponse(
        String scheduleId,
        String tenantId,
        String name,
        boolean enabled,
        String scheduleType,
        String targetType,
        String targetRefId,
        LocalDateTime nextRunAt,
        LocalDateTime updatedAt
) {
}
