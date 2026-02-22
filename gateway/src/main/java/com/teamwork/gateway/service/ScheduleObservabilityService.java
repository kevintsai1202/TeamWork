package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.ScheduleObservabilityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 排程觀測服務：統一記錄 schedule.triggered/completed/failed 與錯誤分類。
 */
@Service
@Slf4j
public class ScheduleObservabilityService {

    private final LongAdder triggeredCount = new LongAdder();
    private final LongAdder completedCount = new LongAdder();
    private final LongAdder failedCount = new LongAdder();
    private final LongAdder totalDurationMs = new LongAdder();

    private final ConcurrentHashMap<String, LongAdder> failedByCategory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> targetTypeCounts = new ConcurrentHashMap<>();

    /**
     * 記錄排程觸發事件。
     */
    public void recordTriggered(String scheduleId, String triggerType, String targetType) {
        triggeredCount.increment();
        targetTypeCounts.computeIfAbsent(normalize(targetType), key -> new LongAdder()).increment();
        log.info("schedule.triggered scheduleId={}, triggerType={}, targetType={}",
                scheduleId,
                triggerType,
                normalize(targetType));
    }

    /**
     * 記錄排程完成事件。
     */
    public void recordCompleted(String scheduleId, String triggerType, long durationMs) {
        completedCount.increment();
        totalDurationMs.add(Math.max(durationMs, 0L));
        log.info("schedule.completed scheduleId={}, triggerType={}, durationMs={}",
                scheduleId,
                triggerType,
                durationMs);
    }

    /**
     * 記錄排程失敗事件。
     */
    public void recordFailed(String scheduleId, String triggerType, String errorCategory) {
        failedCount.increment();
        String category = normalize(errorCategory);
        failedByCategory.computeIfAbsent(category, key -> new LongAdder()).increment();
        log.warn("schedule.failed scheduleId={}, triggerType={}, errorCategory={}",
                scheduleId,
                triggerType,
                category);
    }

    /**
     * 分類錯誤型別，對齊既有 VALIDATION/CONFIGURATION/TIMEOUT/RUNTIME/INTERNAL。
     */
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

    /**
     * 取得目前排程觀測快照。
     */
    public ScheduleObservabilityResponse snapshot() {
        long completed = completedCount.sum();
        long avgDuration = completed == 0 ? 0L : totalDurationMs.sum() / completed;

        Map<String, Long> failures = new HashMap<>();
        failedByCategory.forEach((key, value) -> failures.put(key, value.sum()));

        Map<String, Long> targetTypes = new HashMap<>();
        targetTypeCounts.forEach((key, value) -> targetTypes.put(key, value.sum()));

        return new ScheduleObservabilityResponse(
                triggeredCount.sum(),
                completed,
                failedCount.sum(),
                avgDuration,
                failures,
                targetTypes);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.trim().toUpperCase();
    }
}
