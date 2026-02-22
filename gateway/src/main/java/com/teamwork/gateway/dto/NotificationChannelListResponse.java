package com.teamwork.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通知通道列表回應 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationChannelListResponse {
    private List<NotificationChannelResponse> channels;
}
