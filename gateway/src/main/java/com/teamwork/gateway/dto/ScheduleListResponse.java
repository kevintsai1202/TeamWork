package com.teamwork.gateway.dto;

import java.util.List;

public record ScheduleListResponse(List<ScheduleSummaryResponse> items) {
}
