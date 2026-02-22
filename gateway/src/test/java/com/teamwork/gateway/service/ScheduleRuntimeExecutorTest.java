package com.teamwork.gateway.service;

import com.teamwork.gateway.entity.TaskSchedule;
import com.teamwork.gateway.event.ScheduleTriggeredEvent;
import com.teamwork.gateway.repository.TaskScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleRuntimeExecutorTest {

    @Mock
    private TaskScheduleRepository taskScheduleRepository;

    @Mock
    private TaskScheduler scheduleTaskScheduler;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
        private ScheduledFuture<?> scheduledFuture;

    @Test
    void reloadAllEnabled_ShouldRegisterAllEnabledSchedules() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.setId("schedule-1");
        schedule.setEnabled(true);
        schedule.setScheduleType("INTERVAL");
        schedule.setIntervalSeconds(60);

        when(taskScheduleRepository.findByEnabled(true)).thenReturn(List.of(schedule));
        doReturn(scheduledFuture)
                .when(scheduleTaskScheduler)
                .schedule(any(Runnable.class), any(org.springframework.scheduling.Trigger.class));

        ScheduleRuntimeExecutor executor = new ScheduleRuntimeExecutor(
                taskScheduleRepository,
                scheduleTaskScheduler,
                eventPublisher);

        executor.reloadAllEnabled();

        verify(scheduleTaskScheduler).schedule(any(Runnable.class), any(org.springframework.scheduling.Trigger.class));
    }

    @Test
    void reloadSchedule_ShouldCancelOldRuntimeBeforeRegisteringNewOne() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.setId("schedule-1");
        schedule.setEnabled(true);
        schedule.setScheduleType("INTERVAL");
        schedule.setIntervalSeconds(30);

        doReturn(scheduledFuture)
                .when(scheduleTaskScheduler)
                .schedule(any(Runnable.class), any(org.springframework.scheduling.Trigger.class));

        ScheduleRuntimeExecutor executor = new ScheduleRuntimeExecutor(
                taskScheduleRepository,
                scheduleTaskScheduler,
                eventPublisher);

        executor.reloadSchedule(schedule);
        executor.reloadSchedule(schedule);

        verify(scheduledFuture).cancel(false);
    }

    @Test
    void reloadSchedule_ShouldPublishScheduleTriggeredEventWhenTaskRuns() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.setId("schedule-1");
        schedule.setEnabled(true);
        schedule.setScheduleType("CRON");
        schedule.setCronExpr("0/30 * * * * *");

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(scheduledFuture)
                .when(scheduleTaskScheduler)
                .schedule(runnableCaptor.capture(), any(org.springframework.scheduling.Trigger.class));

        ScheduleRuntimeExecutor executor = new ScheduleRuntimeExecutor(
                taskScheduleRepository,
                scheduleTaskScheduler,
                eventPublisher);

        executor.reloadSchedule(schedule);
        runnableCaptor.getValue().run();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
                assertThat(eventCaptor.getValue()).isInstanceOf(ScheduleTriggeredEvent.class);
                ScheduleTriggeredEvent event = (ScheduleTriggeredEvent) eventCaptor.getValue();
                assertThat(event.scheduleId()).isEqualTo("schedule-1");
                assertThat(event.triggerType()).isEqualTo("SCHEDULE");
    }
}
