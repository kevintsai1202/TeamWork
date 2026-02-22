package com.teamwork.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 通知策略主檔：描述哪些事件時機需要通知，以及要發送到哪些通道。
 * 一個策略可綁定多個通道（以 JSON 陣列儲存），並可掛載到排程或 Webhook trigger。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "notification_policies")
public class NotificationPolicy {

    @Id
    private String id = UUID.randomUUID().toString();

    /** 租戶 ID */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** 策略顯示名稱 */
    @Column(nullable = false)
    private String name;

    /** 是否在任務啟動時通知 */
    @Column(name = "on_started", nullable = false)
    private boolean onStarted = false;

    /** 是否在任務成功時通知 */
    @Column(name = "on_success", nullable = false)
    private boolean onSuccess = true;

    /** 是否在任務失敗時通知 */
    @Column(name = "on_failed", nullable = false)
    private boolean onFailed = true;

    /** 是否在任務逾時時通知 */
    @Column(name = "on_timeout", nullable = false)
    private boolean onTimeout = true;

    /**
     * 綁定的通知通道 ID 清單（JSON 陣列字串）。
     * 範例：["nc_001","nc_002"]
     */
    @Column(name = "channel_ids_json", columnDefinition = "TEXT")
    private String channelIdsJson;

    /**
     * 訊息模板 ID（可選）。
     * 若為 null 則使用預設模板。
     */
    @Column(name = "template_id")
    private String templateId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
