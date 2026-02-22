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
 * 任務觸發主檔：統一管理 SCHEDULE/WEBHOOK/MANUAL 的觸發設定。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "task_triggers")
public class TaskTrigger {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "trigger_source", nullable = false, length = 20)
    private String triggerSource; // SCHEDULE / WEBHOOK / MANUAL

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType; // AGENT / TOOL / SKILL

    @Column(name = "target_ref_id", nullable = false, length = 120)
    private String targetRefId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "secret_ref")
    private String secretRef;

    @Column(name = "webhook_key", unique = true)
    private String webhookKey;

    @Column(name = "idempotency_ttl_seconds")
    private Integer idempotencyTtlSeconds = 3600;

    @Column(name = "notification_policy_id")
    private String notificationPolicyId;

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
