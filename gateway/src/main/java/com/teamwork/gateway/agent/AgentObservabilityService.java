package com.teamwork.gateway.agent;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Agent 觀測服務：提供最小可用的 metrics 計數與錯誤分類能力。
 */
@Service
@Slf4j
public class AgentObservabilityService {

    private final LongAdder startedCount = new LongAdder();
    private final LongAdder completedCount = new LongAdder();
    private final LongAdder failedCount = new LongAdder();
    private final LongAdder totalDurationMs = new LongAdder();

    private final ConcurrentHashMap<String, LongAdder> failedByCategory = new ConcurrentHashMap<>();

    public void recordTaskStarted(String taskId) {
        startedCount.increment();
        log.info("obs.task.started taskId={}, traceId={}", taskId, currentTraceId());
    }

    public void recordTaskCompleted(String taskId, long durationMs) {
        completedCount.increment();
        totalDurationMs.add(Math.max(durationMs, 0L));
        log.info("obs.task.completed taskId={}, durationMs={}, traceId={}", taskId, durationMs, currentTraceId());
    }

    public void recordTaskFailed(String taskId, Throwable throwable) {
        failedCount.increment();
        String category = classifyError(throwable);
        failedByCategory.computeIfAbsent(category, key -> new LongAdder()).increment();
        log.warn("obs.task.failed taskId={}, errorCategory={}, errorType={}, traceId={}",
                taskId,
                category,
                throwable == null ? "UNKNOWN" : throwable.getClass().getSimpleName(),
                currentTraceId());
    }

    public String classifyError(Throwable throwable) {
        if (throwable == null) {
            return "UNKNOWN";
        }
        if (throwable instanceof IllegalArgumentException) {
            return "VALIDATION";
        }
        if (throwable instanceof IllegalStateException) {
            return "CONFIGURATION";
        }
        if (throwable instanceof RuntimeException) {
            String message = throwable.getMessage();
            if (message != null && message.toLowerCase().contains("timeout")) {
                return "TIMEOUT";
            }
            return "RUNTIME";
        }
        return "INTERNAL";
    }

    public AgentObservabilitySnapshot snapshot() {
        long completed = completedCount.sum();
        long avgDuration = completed == 0 ? 0 : totalDurationMs.sum() / completed;

        Map<String, Long> failures = new HashMap<>();
        failedByCategory.forEach((key, value) -> failures.put(key, value.sum()));

        return new AgentObservabilitySnapshot(
                startedCount.sum(),
                completed,
                failedCount.sum(),
                avgDuration,
                failures);
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? "N/A" : traceId;
    }
}
