package com.teamwork.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通知派送紀錄列表回應 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryListResponse {
    private List<NotificationDeliveryItem> deliveries;
    private int total;
}
