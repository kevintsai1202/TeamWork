package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.event.TaskStatusChangeEvent;
import com.teamwork.gateway.repository.TaskRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MasterAgentAskUserQuestionToolTest {

    @Test
    void askUserQuestion_ShouldSetWaitingStatusAndPublishEvent() {
        TaskRecordRepository repository = mock(TaskRecordRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        PendingUserQuestionStore store = new PendingUserQuestionStore();
        AskUserToolPolicyService policyService = mock(AskUserToolPolicyService.class);
        when(policyService.canAskQuestion()).thenReturn(true);

        TaskRecord task = new TaskRecord();
        task.setId("task-1");
        task.setStatus("RUNNING");

        when(repository.findById("task-1")).thenReturn(Optional.of(task));
        when(repository.save(any(TaskRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MasterAgentTools tools = new MasterAgentTools(null, repository, publisher, store, policyService);

        String result = tools.askUserQuestion("task-1", "請提供部署環境資訊");

        assertThat(result).contains("Question sent");
        assertThat(task.getStatus()).isEqualTo("WAITING_USER_INPUT");
        assertThat(store.get("task-1")).isEqualTo("請提供部署環境資訊");
        verify(publisher, times(1)).publishEvent(any(TaskStatusChangeEvent.class));
    }

    @Test
    void submitUserAnswer_ShouldResumeRunningAndClearPendingQuestion() {
        TaskRecordRepository repository = mock(TaskRecordRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        PendingUserQuestionStore store = new PendingUserQuestionStore();
        AskUserToolPolicyService policyService = mock(AskUserToolPolicyService.class);
        when(policyService.canSubmitAnswer()).thenReturn(true);
        store.put("task-2", "你要使用哪個 region?");

        TaskRecord task = new TaskRecord();
        task.setId("task-2");
        task.setStatus("WAITING_USER_INPUT");

        when(repository.findById("task-2")).thenReturn(Optional.of(task));
        when(repository.save(any(TaskRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MasterAgentTools tools = new MasterAgentTools(null, repository, publisher, store, policyService);

        String result = tools.submitUserAnswer("task-2", "eastus");

        assertThat(result).contains("Answer accepted");
        assertThat(task.getStatus()).isEqualTo("RUNNING");
        assertThat(store.get("task-2")).isNull();
        verify(publisher, times(1)).publishEvent(any(TaskStatusChangeEvent.class));
    }

    @Test
    void listPendingUserQuestions_ShouldReturnCommaSeparatedTaskIds() {
        PendingUserQuestionStore store = new PendingUserQuestionStore();
        AskUserToolPolicyService policyService = mock(AskUserToolPolicyService.class);
        when(policyService.canListPending()).thenReturn(true);
        store.put("task-a", "q1");
        store.put("task-b", "q2");

        MasterAgentTools tools = new MasterAgentTools(null, null, null, store, policyService);

        String result = tools.listPendingUserQuestions();

        assertThat(result).contains("task-a").contains("task-b");
    }

    @Test
    void askUserQuestion_WhenDisabled_ShouldReturnDisabledMessage() {
        AskUserToolPolicyService policyService = mock(AskUserToolPolicyService.class);
        when(policyService.canAskQuestion()).thenReturn(false);
        MasterAgentTools tools = new MasterAgentTools(null, null, null, new PendingUserQuestionStore(), policyService);

        String result = tools.askUserQuestion("task-9", "q");

        assertThat(result).isEqualTo("askUserQuestion tool is disabled");
    }
}
