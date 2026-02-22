package com.teamwork.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通知策略列表回應 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPolicyListResponse {
    private List<NotificationPolicyResponse> policies;
}
