package com.teamwork.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 通知派送紀錄：記錄每一筆通知的發送嘗試狀態。
 * 提供重試退避與死信（DLQ）追蹤能力。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "notification_deliveries")
public class NotificationDelivery {

    @Id
    private String id = UUID.randomUUID().toString();

    /** 租戶 ID */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** 關聯的排程執行 ID（可選，來自 schedule trigger） */
    @Column(name = "run_id")
    private String runId;

    /** 來源識別鍵（用於 dedup，格式：scheduleId/triggerId） */
    @Column(name = "source_ref_id")
    private String sourceRefId;

    /**
     * 事件類型：ON_STARTED / ON_SUCCESS / ON_FAILED / ON_TIMEOUT
     */
    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    /** 通知通道 ID */
    @Column(name = "channel_id", nullable = false)
    private String channelId;

    /**
     * 派送狀態：PENDING / SENT / FAILED / DROPPED
     * DROPPED：重試次數已達上限（進入 DLQ）
     */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    /** 已嘗試次數 */
    @Column(nullable = false)
    private int attempt = 0;

    /** 下次重試時間（FAILED 狀態使用） */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /** 失敗原因（最後一次失敗訊息） */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 實際送出的通知訊息摘要（用於稽核） */
    @Column(name = "payload_snapshot", columnDefinition = "TEXT")
    private String payloadSnapshot;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
