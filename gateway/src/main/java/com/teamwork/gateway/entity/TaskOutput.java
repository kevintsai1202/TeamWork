package com.teamwork.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "task_outputs")
public class TaskOutput {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "output_type", nullable = false)
    private String outputType; // MARKDOWN, JSON, CODE_DIFF

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_blob", columnDefinition = "jsonb")
    private String contentBlob;

}
