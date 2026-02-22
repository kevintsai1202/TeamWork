package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.ScheduleRunNowResponse;
import com.teamwork.gateway.dto.ScheduleUpsertRequest;
import com.teamwork.gateway.dto.ScheduleSummaryResponse;
import com.teamwork.gateway.entity.ScheduleContextSnapshot;
import com.teamwork.gateway.entity.ScheduleRun;
import com.teamwork.gateway.entity.TaskSchedule;
import com.teamwork.gateway.event.ScheduleTriggeredEvent;
import com.teamwork.gateway.repository.ScheduleContextSnapshotRepository;
import com.teamwork.gateway.repository.ScheduleRunRepository;
import com.teamwork.gateway.repository.TaskScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private TaskScheduleRepository taskScheduleRepository;

    @Mock
    private ScheduleRunRepository scheduleRunRepository;

    @Mock
    private ScheduleContextSnapshotRepository scheduleContextSnapshotRepository;

        @Mock
        private ScheduleRuntimeExecutor scheduleRuntimeExecutor;

        @Mock
        private ScheduleTargetDispatchService scheduleTargetDispatchService;

        @Mock
        private ScheduleObservabilityService scheduleObservabilityService;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void runNow_ShouldLazyLoadSharedContextBySegmentKeyAndSaveNewSnapshot() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.setId("schedule-1");
        schedule.setTenantId("tenant-a");
        schedule.setTargetType("AGENT");
        schedule.setTargetRefId("agent-007");
        schedule.setContextMode("SHARED");
        schedule.setPayloadJson("{\"message\":\"hello\"}");

        ScheduleContextSnapshot previous = new ScheduleContextSnapshot();
        previous.setContextSummary("last-context");

        when(taskScheduleRepository.findById("schedule-1")).thenReturn(Optional.of(schedule));
        when(scheduleContextSnapshotRepository.findTopByScheduleIdAndContextSegmentKeyOrderByCreatedAtDesc(
                "schedule-1", "AGENT:agent-007")).thenReturn(Optional.of(previous));
        when(scheduleTargetDispatchService.dispatch(any(TaskSchedule.class), any(Optional.class)))
                .thenReturn(new ScheduleTargetDispatchService.DispatchResult(
                        "task-1", "AGENT", "profileId=agent-007", "ok"));
        when(scheduleRunRepository.save(any(ScheduleRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskScheduleRepository.save(any(TaskSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scheduleContextSnapshotRepository.save(any(ScheduleContextSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(scheduleContextSnapshotRepository.findByScheduleIdAndContextSegmentKeyOrderByCreatedAtDesc(
                "schedule-1", "AGENT:agent-007")).thenReturn(List.of());

        ScheduleRunNowResponse response = scheduleService.runNow("schedule-1");

        assertThat(response.scheduleId()).isEqualTo("schedule-1");
        assertThat(response.status()).isEqualTo("SUCCESS");

        verify(scheduleContextSnapshotRepository)
                .findTopByScheduleIdAndContextSegmentKeyOrderByCreatedAtDesc("schedule-1", "AGENT:agent-007");
        verify(scheduleContextSnapshotRepository).save(any(ScheduleContextSnapshot.class));
        verify(scheduleRunRepository, times(2)).save(any(ScheduleRun.class));
        verify(scheduleObservabilityService).recordTriggered("schedule-1", "MANUAL", "AGENT");
        verify(scheduleObservabilityService).recordCompleted(eq("schedule-1"), eq("MANUAL"), anyLong());

        ArgumentCaptor<ScheduleContextSnapshot> snapshotCaptor = ArgumentCaptor.forClass(ScheduleContextSnapshot.class);
        verify(scheduleContextSnapshotRepository).save(snapshotCaptor.capture());
        ScheduleContextSnapshot captured = snapshotCaptor.getValue();

        assertThat(captured.getScheduleId()).isEqualTo("schedule-1");
        assertThat(captured.getTenantId()).isEqualTo("tenant-a");
        assertThat(captured.getContextSegmentKey()).isEqualTo("AGENT:agent-007");
        assertThat(captured.getContextSummary()).contains("last-context");
    }

    @Test
    void listSchedules_ShouldApplyTenantEnabledAndTargetTypeFilter() {
        TaskSchedule schedule1 = new TaskSchedule();
        schedule1.setId("s1");
        schedule1.setTenantId("tenant-a");
        schedule1.setName("job-1");
        schedule1.setEnabled(true);
        schedule1.setScheduleType("INTERVAL");
        schedule1.setTargetType("AGENT");
        schedule1.setTargetRefId("a1");

        TaskSchedule schedule2 = new TaskSchedule();
        schedule2.setId("s2");
        schedule2.setTenantId("tenant-a");
        schedule2.setName("job-2");
        schedule2.setEnabled(true);
        schedule2.setScheduleType("INTERVAL");
        schedule2.setTargetType("TOOL");
        schedule2.setTargetRefId("t1");

        when(taskScheduleRepository.findByTenantIdAndEnabled("tenant-a", true))
                .thenReturn(List.of(schedule1, schedule2));

        List<ScheduleSummaryResponse> items = scheduleService.listSchedules("tenant-a", true, "AGENT").items();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).scheduleId()).isEqualTo("s1");
    }

        @Test
        void createSchedule_ShouldReloadRuntimeSchedule() {
                ScheduleUpsertRequest request = new ScheduleUpsertRequest();
                request.setTenantId("tenant-a");
                request.setName("job-1");
                request.setEnabled(true);
                request.setScheduleType("INTERVAL");
                request.setIntervalSeconds(120);
                request.setTimezone("Asia/Taipei");
                request.setTargetType("AGENT");
                request.setTargetRefId("agent-001");
                request.setPayloadJson("{\"msg\":\"hello\"}");
                request.setPriority(5);
                request.setMaxConcurrentRuns(1);
                request.setContextMode("ISOLATED");
                request.setContextRetentionRuns(20);
                request.setContextMaxTokens(8000);

                when(taskScheduleRepository.save(any(TaskSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

                scheduleService.createSchedule(request);

                verify(scheduleRuntimeExecutor).reloadSchedule(any(TaskSchedule.class));
        }

        @Test
        void deleteSchedule_ShouldRemoveRuntimeSchedule() {
                TaskSchedule schedule = new TaskSchedule();
                schedule.setId("schedule-1");

                when(taskScheduleRepository.findById("schedule-1")).thenReturn(Optional.of(schedule));

                scheduleService.deleteSchedule("schedule-1");

                verify(scheduleRuntimeExecutor).removeSchedule("schedule-1");
        }

    @Test
    void onScheduleTriggered_ShouldExecuteAsScheduleTrigger() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.setId("schedule-1");
        schedule.setTenantId("tenant-a");
        schedule.setTargetType("AGENT");
        schedule.setTargetRefId("agent-007");
        schedule.setContextMode("ISOLATED");

        when(taskScheduleRepository.findById("schedule-1")).thenReturn(Optional.of(schedule));
        when(scheduleTargetDispatchService.dispatch(any(TaskSchedule.class), any(Optional.class)))
                .thenReturn(new ScheduleTargetDispatchService.DispatchResult(
                        "task-1", "AGENT", "profileId=agent-007", "ok"));
        when(scheduleRunRepository.save(any(ScheduleRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskScheduleRepository.save(any(TaskSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduleService.onScheduleTriggered(new ScheduleTriggeredEvent("schedule-1", "SCHEDULE"));

        ArgumentCaptor<ScheduleRun> runCaptor = ArgumentCaptor.forClass(ScheduleRun.class);
        verify(scheduleRunRepository, times(2)).save(runCaptor.capture());
        assertThat(runCaptor.getAllValues().get(0).getTriggerType()).isEqualTo("SCHEDULE");
        verify(scheduleContextSnapshotRepository, never()).save(any(ScheduleContextSnapshot.class));
    }
}
