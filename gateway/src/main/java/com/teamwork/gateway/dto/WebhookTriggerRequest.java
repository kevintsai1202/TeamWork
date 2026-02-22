package com.teamwork.gateway.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WebhookTriggerRequest {
    private String inputPayload;
    private String idempotencyKey;
}
