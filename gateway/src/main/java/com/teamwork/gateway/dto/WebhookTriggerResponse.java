package com.teamwork.gateway.dto;

public record WebhookTriggerResponse(
        String triggerSource,
        String runId,
        String status
) {
}
