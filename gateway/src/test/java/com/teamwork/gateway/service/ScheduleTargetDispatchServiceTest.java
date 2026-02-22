package com.teamwork.gateway.service;

import com.teamwork.gateway.agent.MasterAgent;
import com.teamwork.gateway.agent.SkillsCatalogService;
import com.teamwork.gateway.entity.AgentProfile;
import com.teamwork.gateway.entity.ScheduleContextSnapshot;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.entity.TaskSchedule;
import com.teamwork.gateway.entity.ToolConfig;
import com.teamwork.gateway.repository.AgentProfileRepository;
import com.teamwork.gateway.repository.TaskRecordRepository;
import com.teamwork.gateway.repository.ToolConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleTargetDispatchServiceTest {

    @Mock
    private AgentProfileRepository agentProfileRepository;

    @Mock
    private ToolConfigRepository toolConfigRepository;

    @Mock
    private SkillsCatalogService skillsCatalogService;

    @Mock
    private TaskRecordRepository taskRecordRepository;

    @Mock
    private MasterAgent masterAgent;

    @InjectMocks
    private ScheduleTargetDispatchService dispatchService;

    @Test
    void dispatchAgent_ShouldCreateTaskAndProcess() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.setTargetType("AGENT");
        schedule.setTargetRefId("profile-1");
        schedule.setPayloadJson("{\"task\":\"hello\"}");

        TaskRecord saved = new TaskRecord();
        saved.setId("task-1");
        saved.setInputPayload("payload");

        when(agentProfileRepository.existsById("profile-1")).thenReturn(true);
        when(taskRecordRepository.save(any(TaskRecord.class))).thenReturn(saved);

        ScheduleTargetDispatchService.DispatchResult result = dispatchService.dispatch(schedule, Optional.empty());

        assertThat(result.targetType()).isEqualTo("AGENT");
        assertThat(result.taskId()).isEqualTo("task-1");
        verify(masterAgent).processTask("task-1", "payload");
    }

    @Test
    void dispatchTool_ShouldResolveByNameAndIncludeSharedContext() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.setTargetType("TOOL");
        schedule.setTargetRefId("master-agent-tools");
        schedule.setPayloadJson("{\"task\":\"hello\"}");

        ToolConfig tool = new ToolConfig();
        tool.setName("master-agent-tools");

        AgentProfile profile = new AgentProfile();
        profile.setId("profile-fallback");

        ScheduleContextSnapshot previous = new ScheduleContextSnapshot();
        previous.setContextSummary("last-context");

        TaskRecord saved = new TaskRecord();
        saved.setId("task-2");
        saved.setInputPayload("tool-payload");

        when(toolConfigRepository.findById("master-agent-tools")).thenReturn(Optional.empty());
        when(toolConfigRepository.findByName("master-agent-tools")).thenReturn(Optional.of(tool));
        when(agentProfileRepository.findAll()).thenReturn(List.of(profile));
        when(taskRecordRepository.save(any(TaskRecord.class))).thenReturn(saved);

        ScheduleTargetDispatchService.DispatchResult result = dispatchService.dispatch(schedule, Optional.of(previous));

        assertThat(result.targetType()).isEqualTo("TOOL");

        ArgumentCaptor<TaskRecord> taskCaptor = ArgumentCaptor.forClass(TaskRecord.class);
        verify(taskRecordRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getInputPayload()).contains("[SHARED_CONTEXT]last-context");
        verify(masterAgent).processTask("task-2", "tool-payload");
    }

    @Test
    void dispatchSkill_ShouldThrowWhenSkillMissing() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.setTargetType("SKILL");
        schedule.setTargetRefId("unknown-skill");

        when(skillsCatalogService.readSkill("unknown-skill"))
                .thenThrow(new IllegalArgumentException("Skill not found: unknown-skill"));

        assertThatThrownBy(() -> dispatchService.dispatch(schedule, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Skill not found");
    }
}
