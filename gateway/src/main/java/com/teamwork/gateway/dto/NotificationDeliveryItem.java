package com.teamwork.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知派送紀錄單筆 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryItem {
    private String id;
    private String runId;
    private String sourceRefId;
    private String eventType;
    private String channelId;
    private String status;
    private int attempt;
    private LocalDateTime nextRetryAt;
    private String errorMessage;
    private LocalDateTime createdAt;
}
