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

@Data
@NoArgsConstructor
@Entity
@Table(name = "context_deletion_audits")
public class ContextDeletionAudit {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "mode", nullable = false, length = 30)
    private String mode;

    @Column(name = "removed_count", nullable = false)
    private Integer removedCount;

    @Column(name = "from_index")
    private Integer fromIndex;

    @Column(name = "to_index")
    private Integer toIndex;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "operator_id")
    private String operatorId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
