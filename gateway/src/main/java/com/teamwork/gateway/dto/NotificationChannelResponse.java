package com.teamwork.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知通道回應 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationChannelResponse {
    private String id;
    private String tenantId;
    private String name;
    private String channelType;
    private String endpointConfigJson;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
