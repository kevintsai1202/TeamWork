package com.teamwork.gateway.service;

import com.teamwork.gateway.entity.TaskSchedule;
import com.teamwork.gateway.event.ScheduleTriggeredEvent;
import com.teamwork.gateway.repository.TaskScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 排程 runtime 執行器：負責動態註冊、重載、移除排程觸發器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleRuntimeExecutor {

    private final TaskScheduleRepository taskScheduleRepository;
    private final TaskScheduler scheduleTaskScheduler;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * runtime 註冊表：key 為 scheduleId，value 為可取消的排程任務。
     */
    private final Map<String, ScheduledFuture<?>> runtimeSchedules = new ConcurrentHashMap<>();

    /**
     * 啟動時重載所有啟用中的排程。
     */
    @PostConstruct
    public void reloadAllEnabled() {
        taskScheduleRepository.findByEnabled(true).forEach(this::reloadSchedule);
    }

    /**
     * 依照最新設定重載單筆排程；若停用則移除 runtime 任務。
     */
    public void reloadSchedule(TaskSchedule schedule) {
        if (schedule == null || schedule.getId() == null) {
            return;
        }

        cancelExisting(schedule.getId());

        if (!schedule.isEnabled()) {
            return;
        }

        try {
            ScheduledFuture<?> future = scheduleTaskScheduler.schedule(
                    () -> publishTriggeredEvent(schedule.getId()),
                    buildTrigger(schedule));
            if (future != null) {
                runtimeSchedules.put(schedule.getId(), future);
            }
        } catch (Exception ex) {
            log.warn("Failed to register runtime schedule: id={}, reason={}", schedule.getId(), ex.getMessage());
        }
    }

    /**
     * 移除單筆排程的 runtime 任務。
     */
    public void removeSchedule(String scheduleId) {
        cancelExisting(scheduleId);
    }

    private void publishTriggeredEvent(String scheduleId) {
        eventPublisher.publishEvent(new ScheduleTriggeredEvent(scheduleId, "SCHEDULE"));
    }

    private void cancelExisting(String scheduleId) {
        ScheduledFuture<?> existing = runtimeSchedules.remove(scheduleId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    private org.springframework.scheduling.Trigger buildTrigger(TaskSchedule schedule) {
        String scheduleType = schedule.getScheduleType() == null
                ? ""
                : schedule.getScheduleType().trim().toUpperCase(Locale.ROOT);

        if ("CRON".equals(scheduleType)) {
            if (schedule.getCronExpr() == null || schedule.getCronExpr().isBlank()) {
                throw new IllegalArgumentException("cronExpr is required for CRON schedule");
            }
            ZoneId zoneId = resolveZoneId(schedule.getTimezone());
            return new CronTrigger(schedule.getCronExpr(), zoneId);
        }

        if ("INTERVAL".equals(scheduleType)) {
            int intervalSeconds = schedule.getIntervalSeconds() == null ? 60 : Math.max(1, schedule.getIntervalSeconds());
            PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(intervalSeconds));
            trigger.setInitialDelay(Duration.ofSeconds(intervalSeconds));
            return trigger;
        }

        throw new IllegalArgumentException("Unsupported scheduleType: " + schedule.getScheduleType());
    }

    private ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception ignored) {
            return ZoneId.systemDefault();
        }
    }
}
