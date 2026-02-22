package com.teamwork.gateway.memory;

public record ContextCompressionMetricsSnapshot(
        long attempts,
        long compressed,
        long failures,
        long totalSavedTokens,
        double failureRate,
        double averageSavedRatio) {
}