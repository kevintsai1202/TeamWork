package com.teamwork.gateway.event;

/**
 * 通知派送失敗事件。
 * 由 NotificationDispatchService 在派送失敗後發布，SSE 可監聽此事件進行前端推播。
 *
 * @param deliveryId  派送紀錄 ID
 * @param runId       關聯執行 ID
 * @param channelType 通道型別（EMAIL / WEBHOOK）
 * @param attempt     已嘗試次數
 * @param error       失敗原因
 */
public record NotificationFailedEvent(
        String deliveryId,
        String runId,
        String channelType,
        int attempt,
        String error) {}
