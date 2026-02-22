package com.teamwork.gateway.dto;

import java.time.LocalDateTime;

public record ScheduleCreateResponse(
        String scheduleId,
        LocalDateTime nextRunAt
) {
}
