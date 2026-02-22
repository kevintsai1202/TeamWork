package com.teamwork.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知策略回應 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPolicyResponse {
    private String id;
    private String tenantId;
    private String name;
    private boolean onStarted;
    private boolean onSuccess;
    private boolean onFailed;
    private boolean onTimeout;
    private List<String> channelIds;
    private String templateId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
