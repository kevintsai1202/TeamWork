package com.teamwork.gateway.dto;

public record ContextMessageItem(
        int index,
        String role,
        String content,
        long estimatedTokens
) {
}
