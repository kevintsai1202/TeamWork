package com.teamwork.gateway.dto;

public record ContextDeleteRequest(
        String mode,
        ContextDeleteRange range,
        String reason
) {
}
