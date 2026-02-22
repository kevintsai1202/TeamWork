package com.teamwork.gateway.dto;

public record ToolCallSummaryItem(
        String toolName,
        String arguments,
        String resultPreview
) {
}
