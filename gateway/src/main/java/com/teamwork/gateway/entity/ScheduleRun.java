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
 * 排程執行紀錄：記錄每次觸發的執行狀態與結果。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "schedule_runs")
public class ScheduleRun {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "schedule_id", nullable = false)
    private String scheduleId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType; // AUTO / MANUAL / RETRY / WEBHOOK

    @Column(nullable = false, length = 20)
    private String status; // PENDING / RUNNING / SUCCESS / FAILED / CANCELLED

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
