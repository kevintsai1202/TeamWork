package com.teamwork.gateway.dto;

public record ContextDeleteResponse(
        String taskId,
        boolean removed,
        int removedCount,
        String auditId
) {
}
