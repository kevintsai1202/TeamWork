package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.ScheduleCreateResponse;
import com.teamwork.gateway.dto.ScheduleListResponse;
import com.teamwork.gateway.dto.ScheduleRunItemResponse;
import com.teamwork.gateway.dto.ScheduleRunListResponse;
import com.teamwork.gateway.dto.ScheduleRunNowResponse;
import com.teamwork.gateway.dto.ScheduleSummaryResponse;
import com.teamwork.gateway.dto.ScheduleUpsertRequest;
import com.teamwork.gateway.dto.ScheduleObservabilityResponse;
import com.teamwork.gateway.entity.ScheduleContextSnapshot;
import com.teamwork.gateway.entity.ScheduleRun;
import com.teamwork.gateway.entity.TaskSchedule;
import com.teamwork.gateway.event.ScheduleTriggeredEvent;
import com.teamwork.gateway.repository.ScheduleContextSnapshotRepository;
import com.teamwork.gateway.repository.ScheduleRunRepository;
import com.teamwork.gateway.repository.TaskScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleService {

    private final TaskScheduleRepository taskScheduleRepository;
    private final ScheduleRunRepository scheduleRunRepository;
    private final ScheduleContextSnapshotRepository scheduleContextSnapshotRepository;
    private final ScheduleRuntimeExecutor scheduleRuntimeExecutor;
    private final ScheduleTargetDispatchService scheduleTargetDispatchService;
    private final ScheduleObservabilityService scheduleObservabilityService;
    /** 通知派送引擎（T21），新增後不影響既有排程邏輯 */
    @Nullable
    private final NotificationDispatchService notificationDispatchService;

    public ScheduleCreateResponse createSchedule(ScheduleUpsertRequest request) {
        validateCreateRequest(request);

        TaskSchedule schedule = new TaskSchedule();
        applyUpsert(schedule, request);
        schedule.setNextRunAt(calculateNextRunAt(schedule));
        TaskSchedule saved = taskScheduleRepository.save(schedule);
        scheduleRuntimeExecutor.reloadSchedule(saved);
        return new ScheduleCreateResponse(saved.getId(), saved.getNextRunAt());
    }

    public ScheduleSummaryResponse updateSchedule(String scheduleId, ScheduleUpsertRequest request) {
        TaskSchedule schedule = getScheduleOrThrow(scheduleId);
        applyUpsert(schedule, request);
        schedule.setNextRunAt(calculateNextRunAt(schedule));
        TaskSchedule saved = taskScheduleRepository.save(schedule);
        scheduleRuntimeExecutor.reloadSchedule(saved);
        return toSummary(saved);
    }

    public ScheduleSummaryResponse enableSchedule(String scheduleId) {
        TaskSchedule schedule = getScheduleOrThrow(scheduleId);
        schedule.setEnabled(true);
        schedule.setNextRunAt(calculateNextRunAt(schedule));
        TaskSchedule saved = taskScheduleRepository.save(schedule);
        scheduleRuntimeExecutor.reloadSchedule(saved);
        return toSummary(saved);
    }

    public ScheduleSummaryResponse disableSchedule(String scheduleId) {
        TaskSchedule schedule = getScheduleOrThrow(scheduleId);
        schedule.setEnabled(false);
        TaskSchedule saved = taskScheduleRepository.save(schedule);
        scheduleRuntimeExecutor.reloadSchedule(saved);
        return toSummary(saved);
    }

    public void deleteSchedule(String scheduleId) {
        TaskSchedule schedule = getScheduleOrThrow(scheduleId);
        taskScheduleRepository.delete(schedule);
        scheduleRuntimeExecutor.removeSchedule(scheduleId);
    }

    public ScheduleListResponse listSchedules(String tenantId, Boolean enabled, String targetType) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }

        List<TaskSchedule> schedules = enabled == null
                ? taskScheduleRepository.findByTenantId(tenantId)
                : taskScheduleRepository.findByTenantIdAndEnabled(tenantId, enabled);

        List<ScheduleSummaryResponse> items = schedules.stream()
                .filter(schedule -> !isSpecified(targetType)
                        || targetType.trim().equalsIgnoreCase(schedule.getTargetType()))
                .map(this::toSummary)
                .toList();
        return new ScheduleListResponse(items);
    }

    public ScheduleRunNowResponse runNow(String scheduleId) {
        TaskSchedule schedule = getScheduleOrThrow(scheduleId);
        return executeScheduleRun(schedule, "MANUAL");
    }

    public ScheduleObservabilityResponse getObservabilitySnapshot() {
        return scheduleObservabilityService.snapshot();
    }

    /**
     * 處理 runtime scheduler 觸發事件，統一進入排程執行流程。
     */
    @EventListener
    public void onScheduleTriggered(ScheduleTriggeredEvent event) {
        try {
            TaskSchedule schedule = getScheduleOrThrow(event.scheduleId());
            executeScheduleRun(schedule, event.triggerType());
        } catch (Exception ex) {
            log.warn("Failed to execute scheduled run: scheduleId={}, reason={}", event.scheduleId(), ex.getMessage());
        }
    }

    /**
     * 執行排程（手動或系統觸發）並寫入 run/context snapshot。
     */
    private ScheduleRunNowResponse executeScheduleRun(TaskSchedule schedule, String triggerType) {
        String normalizedTriggerType = triggerType == null || triggerType.isBlank() ? "MANUAL" : triggerType;
        long startedAtMs = System.currentTimeMillis();

        ScheduleRun run = new ScheduleRun();
        run.setScheduleId(schedule.getId());
        run.setTenantId(schedule.getTenantId());
        run.setTriggerType(normalizedTriggerType);
        run.setStatus("RUNNING");
        run.setStartedAt(LocalDateTime.now());
        ScheduleRun running = scheduleRunRepository.save(run);

        String segmentKey = buildContextSegmentKey(schedule);
        Optional<ScheduleContextSnapshot> previousSnapshot = Optional.empty();
        String previousContextSummary = "";

        scheduleObservabilityService.recordTriggered(schedule.getId(), normalizedTriggerType, schedule.getTargetType());

        if ("SHARED".equalsIgnoreCase(schedule.getContextMode())) {
            previousSnapshot = scheduleContextSnapshotRepository
                    .findTopByScheduleIdAndContextSegmentKeyOrderByCreatedAtDesc(schedule.getId(), segmentKey);
            previousContextSummary = previousSnapshot
                    .map(ScheduleContextSnapshot::getContextSummary)
                    .orElse("");
        }

        try {
            ScheduleTargetDispatchService.DispatchResult dispatchResult =
                    scheduleTargetDispatchService.dispatch(schedule, previousSnapshot);

            running.setStatus("SUCCESS");
            running.setFinishedAt(LocalDateTime.now());
            running.setDurationMs(Math.max(1, System.currentTimeMillis() - startedAtMs));
            running.setResultSummary("Run completed. targetType="
                    + dispatchResult.targetType()
                    + ", targetSummary=" + dispatchResult.targetSummary()
                    + ", sharedContextLoaded=" + previousSnapshot.isPresent()
                    + ", contextSegmentKey=" + segmentKey);
            scheduleRunRepository.save(running);

            if ("SHARED".equalsIgnoreCase(schedule.getContextMode())) {
                saveSharedSnapshotWithPolicies(schedule, running, segmentKey, previousContextSummary, dispatchResult);
            }

            schedule.setNextRunAt(calculateNextRunAt(schedule));
            taskScheduleRepository.save(schedule);
            scheduleObservabilityService.recordCompleted(schedule.getId(), normalizedTriggerType, running.getDurationMs());
            // T21：排程執行成功後觸發通知派送
            if (notificationDispatchService != null) {
                notificationDispatchService.dispatchForScheduleRun(running, schedule);
            }

            return new ScheduleRunNowResponse(schedule.getId(), running.getId(), running.getStatus());
        } catch (Exception ex) {
            String category = scheduleObservabilityService.classifyError(ex);
            running.setStatus("FAILED");
            running.setFinishedAt(LocalDateTime.now());
            running.setDurationMs(Math.max(1, System.currentTimeMillis() - startedAtMs));
            running.setErrorCode("SCHEDULE_" + category);
            running.setErrorMessage(ex.getMessage());
            running.setResultSummary("Run failed. errorCategory=" + category);
            scheduleRunRepository.save(running);

            schedule.setNextRunAt(calculateNextRunAt(schedule));
            taskScheduleRepository.save(schedule);

            scheduleObservabilityService.recordFailed(schedule.getId(), normalizedTriggerType, category);
            // T21：排程執行失敗後觸發通知派送
            if (notificationDispatchService != null) {
                notificationDispatchService.dispatchForScheduleRun(running, schedule);
            }
            return new ScheduleRunNowResponse(schedule.getId(), running.getId(), running.getStatus());
        }
    }

    private void saveSharedSnapshotWithPolicies(
            TaskSchedule schedule,
            ScheduleRun running,
            String segmentKey,
            String previousContextSummary,
            ScheduleTargetDispatchService.DispatchResult dispatchResult) {

        ScheduleContextSnapshot snapshot = new ScheduleContextSnapshot();
        snapshot.setScheduleId(schedule.getId());
        snapshot.setRunId(running.getId());
        snapshot.setTaskId(dispatchResult.taskId());
        snapshot.setContextSegmentKey(segmentKey);
        snapshot.setTenantId(schedule.getTenantId());
        snapshot.setMessageCount(1);
        snapshot.setContextSummary(buildCurrentContextSummary(previousContextSummary, schedule));
        snapshot.setToolResultSummary(dispatchResult.resultSummary());
        snapshot.setPendingTodos("");

        int estimatedTokens = estimateTokens(snapshot.getContextSummary()) + estimateTokens(snapshot.getToolResultSummary());
        int maxTokens = schedule.getContextMaxTokens() == null ? 8000 : Math.max(200, schedule.getContextMaxTokens());
        if (estimatedTokens > maxTokens) {
            snapshot.setContextSummary(compressContextSummary(snapshot.getContextSummary(), maxTokens));
            estimatedTokens = estimateTokens(snapshot.getContextSummary()) + estimateTokens(snapshot.getToolResultSummary());
        }
        snapshot.setEstimatedTokens(estimatedTokens);

        scheduleContextSnapshotRepository.save(snapshot);
        enforceRetentionPolicy(schedule, segmentKey);
    }

    private void enforceRetentionPolicy(TaskSchedule schedule, String segmentKey) {
        int retentionRuns = schedule.getContextRetentionRuns() == null ? 20 : Math.max(1, schedule.getContextRetentionRuns());
        List<ScheduleContextSnapshot> snapshots = scheduleContextSnapshotRepository
                .findByScheduleIdAndContextSegmentKeyOrderByCreatedAtDesc(schedule.getId(), segmentKey);
        if (snapshots.size() <= retentionRuns) {
            return;
        }
        List<ScheduleContextSnapshot> toDelete = snapshots.subList(retentionRuns, snapshots.size());
        scheduleContextSnapshotRepository.deleteAll(toDelete);
    }

    private String compressContextSummary(String summary, int maxTokens) {
        if (summary == null || summary.isBlank()) {
            return "";
        }
        int maxChars = Math.max(80, maxTokens * 4);
        if (summary.length() <= maxChars) {
            return summary;
        }
        int headLength = Math.min(maxChars / 2, summary.length());
        int tailLength = Math.min(maxChars / 3, summary.length() - headLength);
        int tailStart = Math.max(headLength, summary.length() - tailLength);
        return "[COMPRESSED] "
                + summary.substring(0, headLength)
                + " ... "
                + summary.substring(tailStart);
    }

    public ScheduleRunListResponse listRuns(String scheduleId) {
        getScheduleOrThrow(scheduleId);
        List<ScheduleRunItemResponse> items = scheduleRunRepository.findByScheduleIdOrderByCreatedAtDesc(scheduleId)
                .stream()
                .map(run -> new ScheduleRunItemResponse(
                        run.getId(),
                        run.getTriggerType(),
                        run.getStatus(),
                        run.getStartedAt(),
                        run.getFinishedAt(),
                        run.getDurationMs()))
                .toList();
        return new ScheduleRunListResponse(items);
    }

    private String buildCurrentContextSummary(String previousSummary, TaskSchedule schedule) {
        String payload = schedule.getPayloadJson() == null ? "" : schedule.getPayloadJson();
        String payloadPreview = payload.length() > 120 ? payload.substring(0, 120) : payload;
        if (previousSummary == null || previousSummary.isBlank()) {
            return "[SCHEDULE_CONTEXT] payload=" + payloadPreview;
        }
        return "[SCHEDULE_CONTEXT] previous=" + previousSummary + " | payload=" + payloadPreview;
    }

    private String buildContextSegmentKey(TaskSchedule schedule) {
        String targetType = schedule.getTargetType() == null ? "UNKNOWN" : schedule.getTargetType().toUpperCase(Locale.ROOT);
        String targetRefId = schedule.getTargetRefId() == null ? "UNKNOWN" : schedule.getTargetRefId();
        return targetType + ":" + targetRefId;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Math.max(1, (text.length() + 3L) / 4L);
    }

    private TaskSchedule getScheduleOrThrow(String scheduleId) {
        return taskScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
    }

    private void validateCreateRequest(ScheduleUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Schedule request cannot be null");
        }
        if (!isSpecified(request.getTenantId())) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (!isSpecified(request.getName())) {
            throw new IllegalArgumentException("name is required");
        }
        if (!isSpecified(request.getScheduleType())) {
            throw new IllegalArgumentException("scheduleType is required");
        }
        if (!isSpecified(request.getTargetType())) {
            throw new IllegalArgumentException("targetType is required");
        }
        if (!isSpecified(request.getTargetRefId())) {
            throw new IllegalArgumentException("targetRefId is required");
        }
    }

    private void applyUpsert(TaskSchedule schedule, ScheduleUpsertRequest request) {
        if (isSpecified(request.getTenantId())) {
            schedule.setTenantId(request.getTenantId());
        }
        if (isSpecified(request.getName())) {
            schedule.setName(request.getName());
        }
        if (request.getEnabled() != null) {
            schedule.setEnabled(request.getEnabled());
        }
        if (isSpecified(request.getScheduleType())) {
            schedule.setScheduleType(request.getScheduleType().toUpperCase(Locale.ROOT));
        }
        if (request.getCronExpr() != null) {
            schedule.setCronExpr(request.getCronExpr());
        }
        if (request.getIntervalSeconds() != null) {
            schedule.setIntervalSeconds(request.getIntervalSeconds());
        }
        if (request.getTimezone() != null) {
            schedule.setTimezone(request.getTimezone());
        }
        if (isSpecified(request.getTargetType())) {
            schedule.setTargetType(request.getTargetType().toUpperCase(Locale.ROOT));
        }
        if (isSpecified(request.getTargetRefId())) {
            schedule.setTargetRefId(request.getTargetRefId());
        }
        if (request.getPayloadJson() != null) {
            schedule.setPayloadJson(request.getPayloadJson());
        }
        if (request.getPriority() != null) {
            schedule.setPriority(request.getPriority());
        }
        if (request.getMaxConcurrentRuns() != null) {
            schedule.setMaxConcurrentRuns(request.getMaxConcurrentRuns());
        }
        if (isSpecified(request.getContextMode())) {
            schedule.setContextMode(request.getContextMode().toUpperCase(Locale.ROOT));
        }
        if (request.getContextRetentionRuns() != null) {
            schedule.setContextRetentionRuns(request.getContextRetentionRuns());
        }
        if (request.getContextMaxTokens() != null) {
            schedule.setContextMaxTokens(request.getContextMaxTokens());
        }
        if (request.getNotificationPolicyId() != null) {
            schedule.setNotificationPolicyId(request.getNotificationPolicyId());
        }
        if (schedule.getCreatedBy() == null) {
            schedule.setCreatedBy("system");
        }
    }

    private LocalDateTime calculateNextRunAt(TaskSchedule schedule) {
        if ("INTERVAL".equalsIgnoreCase(schedule.getScheduleType()) && schedule.getIntervalSeconds() != null) {
            return LocalDateTime.now().plusSeconds(Math.max(60, schedule.getIntervalSeconds()));
        }
        return LocalDateTime.now().plusMinutes(1);
    }

    private ScheduleSummaryResponse toSummary(TaskSchedule schedule) {
        return new ScheduleSummaryResponse(
                schedule.getId(),
                schedule.getTenantId(),
                schedule.getName(),
                schedule.isEnabled(),
                schedule.getScheduleType(),
                schedule.getTargetType(),
                schedule.getTargetRefId(),
                schedule.getNextRunAt(),
                schedule.getUpdatedAt());
    }

    private boolean isSpecified(String value) {
        return value != null && !value.isBlank();
    }
}
