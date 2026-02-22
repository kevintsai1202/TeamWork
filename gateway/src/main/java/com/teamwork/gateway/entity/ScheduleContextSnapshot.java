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
 * 排程上下文快照：保存 SHARED 模式下可回放的上下文摘要。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "schedule_context_snapshots")
public class ScheduleContextSnapshot {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "schedule_id", nullable = false)
    private String scheduleId;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "context_segment_key", length = 180)
    private String contextSegmentKey;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @Column(name = "estimated_tokens", nullable = false)
    private Integer estimatedTokens = 0;

    @Column(name = "context_summary", columnDefinition = "TEXT")
    private String contextSummary;

    @Column(name = "tool_result_summary", columnDefinition = "TEXT")
    private String toolResultSummary;

    @Column(name = "pending_todos", columnDefinition = "TEXT")
    private String pendingTodos;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
