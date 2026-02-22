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
 * 排程主檔：描述一筆可被系統自動觸發的任務設定。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "task_schedules")
public class TaskSchedule {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "schedule_type", nullable = false)
    private String scheduleType; // CRON / INTERVAL

    @Column(name = "cron_expr", length = 120)
    private String cronExpr;

    @Column(name = "interval_seconds")
    private Integer intervalSeconds;

    @Column(length = 60)
    private String timezone;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType; // AGENT / TOOL / SKILL

    @Column(name = "target_ref_id", nullable = false, length = 120)
    private String targetRefId;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "priority", nullable = false)
    private Integer priority = 5;

    @Column(name = "max_concurrent_runs", nullable = false)
    private Integer maxConcurrentRuns = 1;

    @Column(name = "context_mode", nullable = false, length = 20)
    private String contextMode = "ISOLATED"; // ISOLATED / SHARED

    @Column(name = "context_retention_runs", nullable = false)
    private Integer contextRetentionRuns = 20;

    @Column(name = "context_max_tokens", nullable = false)
    private Integer contextMaxTokens = 8000;

    @Column(name = "notification_policy_id")
    private String notificationPolicyId;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "created_by")
    private String createdBy;

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
