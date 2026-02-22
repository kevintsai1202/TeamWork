package com.teamwork.gateway.service;

import com.teamwork.gateway.agent.MasterAgent;
import com.teamwork.gateway.dto.WebhookTriggerRequest;
import com.teamwork.gateway.dto.WebhookTriggerResponse;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.entity.TaskTrigger;
import com.teamwork.gateway.repository.AgentProfileRepository;
import com.teamwork.gateway.repository.TaskRecordRepository;
import com.teamwork.gateway.repository.TaskTriggerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriggerExecutionServiceTest {

    @Mock
    private TaskTriggerRepository taskTriggerRepository;

    @Mock
    private AgentProfileRepository agentProfileRepository;

    @Mock
    private TaskRecordRepository taskRecordRepository;

    @Mock
    private MasterAgent masterAgent;

    @Mock
    private WebhookSecurityService webhookSecurityService;

    @InjectMocks
    private TriggerExecutionService triggerExecutionService;

    @Test
    void executeWebhook_ShouldCreatePendingTaskAndInvokeMasterAgent() {
        TaskTrigger trigger = new TaskTrigger();
        trigger.setEnabled(true);
        trigger.setTriggerSource("WEBHOOK");
        trigger.setTargetType("AGENT");
        trigger.setTargetRefId("profile-1");

        WebhookTriggerRequest request = new WebhookTriggerRequest();
        request.setInputPayload("run report");

        TaskRecord saved = new TaskRecord();
        saved.setId("task-1");
        saved.setStatus("PENDING");
        saved.setInputPayload("run report");

        when(taskTriggerRepository.findByWebhookKey("wh_1")).thenReturn(Optional.of(trigger));
        when(agentProfileRepository.existsById("profile-1")).thenReturn(true);
        when(taskRecordRepository.save(any(TaskRecord.class))).thenReturn(saved);

        WebhookTriggerResponse response = triggerExecutionService.executeWebhook(
            "wh_1",
            request,
            "1700000000",
            "nonce-1",
            "signature-1");

        assertThat(response.triggerSource()).isEqualTo("WEBHOOK");
        assertThat(response.runId()).isEqualTo("task-1");
        assertThat(response.status()).isEqualTo("PENDING");
        verify(webhookSecurityService).validate(
            trigger,
            "wh_1",
            request,
            "1700000000",
            "nonce-1",
            "signature-1");
        verify(masterAgent).processTask("task-1", "run report");
    }

    @Test
    void executeWebhook_ShouldThrowWhenTriggerDisabled() {
        TaskTrigger trigger = new TaskTrigger();
        trigger.setEnabled(false);
        trigger.setTriggerSource("WEBHOOK");

        when(taskTriggerRepository.findByWebhookKey("wh_2")).thenReturn(Optional.of(trigger));

        WebhookTriggerRequest request = new WebhookTriggerRequest();
        request.setInputPayload("x");

        assertThatThrownBy(() -> triggerExecutionService.executeWebhook(
            "wh_2",
            request,
            "1700000000",
            "nonce-2",
            "signature-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trigger is disabled");
    }

    @Test
    void executeWebhook_ShouldThrowWhenInputPayloadBlank() {
        TaskTrigger trigger = new TaskTrigger();
        trigger.setEnabled(true);
        trigger.setTriggerSource("WEBHOOK");
        trigger.setTargetType("AGENT");
        trigger.setTargetRefId("profile-1");

        when(taskTriggerRepository.findByWebhookKey("wh_3")).thenReturn(Optional.of(trigger));
        when(agentProfileRepository.existsById("profile-1")).thenReturn(true);

        WebhookTriggerRequest request = new WebhookTriggerRequest();
        request.setInputPayload(" ");

        assertThatThrownBy(() -> triggerExecutionService.executeWebhook(
            "wh_3",
            request,
            "1700000000",
            "nonce-3",
            "signature-3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inputPayload is required");
    }
}
