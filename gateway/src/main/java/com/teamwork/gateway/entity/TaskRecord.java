package com.teamwork.gateway.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "task_records")
public class TaskRecord {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "parent_task_id")
    private String parentTaskId;

    @Column(name = "profile_id", nullable = false)
    private String profileId;

    @Column(nullable = false)
    private String status; // PENDING, RUNNING, COMPLETED, FAILED

    @Column(name = "input_payload", columnDefinition = "TEXT")
    private String inputPayload;

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
