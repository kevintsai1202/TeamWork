package com.teamwork.gateway.dto;

public record ScheduleRunNowResponse(
        String scheduleId,
        String runId,
        String status
) {
}
