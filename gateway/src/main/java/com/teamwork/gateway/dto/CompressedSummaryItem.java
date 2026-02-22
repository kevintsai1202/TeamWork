package com.teamwork.gateway.dto;

import java.time.LocalDateTime;

public record CompressedSummaryItem(
        String summaryId,
        String content,
        String sourceRange,
        LocalDateTime createdAt
) {
}
