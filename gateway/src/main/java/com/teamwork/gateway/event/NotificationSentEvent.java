package com.teamwork.gateway.event;

/**
 * 通知派送成功事件。
 * 由 NotificationDispatchService 在成功送出一筆通知後發布，SSE 可監聽此事件進行前端推播。
 *
 * @param deliveryId 派送紀錄 ID
 * @param runId      關聯執行 ID
 * @param channelType 通道型別（EMAIL / WEBHOOK）
 * @param eventType  事件類型（ON_SUCCESS / ON_FAILED 等）
 */
public record NotificationSentEvent(String deliveryId, String runId, String channelType, String eventType) {}
