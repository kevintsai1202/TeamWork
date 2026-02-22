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
 * 通知通道主檔：描述通知的接收方設定（Email / Webhook 等）。
 * 每租戶可建立多個通道，並可獨立啟用/停用。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "notification_channels")
public class NotificationChannel {

    @Id
    private String id = UUID.randomUUID().toString();

    /** 租戶 ID */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** 通道顯示名稱 */
    @Column(nullable = false)
    private String name;

    /**
     * 通道型別：EMAIL / WEBHOOK
     * 擴充階段支援：SLACK / DISCORD / LINE / TEAMS
     */
    @Column(name = "channel_type", nullable = false, length = 30)
    private String channelType;

    /**
     * 通道端點設定（JSON）。
     * EMAIL 範例：{"to":["ops@example.com"],"subjectPrefix":"[TeamWork]"}
     * WEBHOOK 範例：{"url":"https://example.com/hook","headers":{"X-Token":"abc"}}
     */
    @Column(name = "endpoint_config_json", columnDefinition = "TEXT")
    private String endpointConfigJson;

    /** 是否啟用 */
    @Column(nullable = false)
    private boolean enabled = true;

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
