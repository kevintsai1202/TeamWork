package com.teamwork.gateway.controller;

import com.teamwork.gateway.dto.WebhookTriggerRequest;
import com.teamwork.gateway.dto.WebhookTriggerResponse;
import com.teamwork.gateway.service.TriggerExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/triggers")
@RequiredArgsConstructor
public class TriggerController {

    private final TriggerExecutionService triggerExecutionService;

    @PostMapping("/webhooks/{webhookKey}")
    public ResponseEntity<WebhookTriggerResponse> triggerByWebhook(
            @PathVariable String webhookKey,
            @RequestBody WebhookTriggerRequest request,
            @RequestHeader("X-Trigger-Timestamp") String triggerTimestamp,
            @RequestHeader("X-Trigger-Nonce") String triggerNonce,
            @RequestHeader("X-Trigger-Signature") String triggerSignature) {
        WebhookTriggerResponse response = triggerExecutionService.executeWebhook(
                webhookKey,
                request,
                triggerTimestamp,
                triggerNonce,
                triggerSignature);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
