package com.teamwork.gateway.controller;

import com.teamwork.gateway.dto.WebhookTriggerRequest;
import com.teamwork.gateway.dto.WebhookTriggerResponse;
import com.teamwork.gateway.service.TriggerExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriggerControllerTest {

    @Mock
    private TriggerExecutionService triggerExecutionService;

    @InjectMocks
    private TriggerController triggerController;

    @Test
    void triggerByWebhook_ShouldReturnAccepted() {
        WebhookTriggerRequest request = new WebhookTriggerRequest();
        request.setInputPayload("do something");

        WebhookTriggerResponse serviceResponse = new WebhookTriggerResponse("WEBHOOK", "task-1", "PENDING");
        when(triggerExecutionService.executeWebhook(
            "wh_1",
            request,
            "1700000000",
            "nonce-1",
            "sig-1")).thenReturn(serviceResponse);

        ResponseEntity<WebhookTriggerResponse> response = triggerController.triggerByWebhook(
            "wh_1",
            request,
            "1700000000",
            "nonce-1",
            "sig-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().runId()).isEqualTo("task-1");
    }
}
