package com.teamwork.gateway;

import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.repository.TaskRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class SchemaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TaskRecordRepository taskRecordRepository;

    @Test
    void testSchemaIsCreatedAndEntityCanBeSaved() {
        // Assert container is running
        assertThat(postgres.isRunning()).isTrue();

        // Arrange
        TaskRecord task = new TaskRecord();
        task.setProfileId("profile-123");
        task.setStatus("PENDING");
        task.setInputPayload("{\"key\":\"value\"}");

        // Act
        TaskRecord savedTask = taskRecordRepository.save(task);

        // Assert
        assertThat(savedTask.getId()).isNotEmpty();
        assertThat(savedTask.getCreatedAt()).isNotNull();
        assertThat(savedTask.getUpdatedAt()).isNotNull();

        // Find it back
        var retrieved = taskRecordRepository.findById(savedTask.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getInputPayload()).isEqualTo("{\"key\":\"value\"}");
        assertThat(retrieved.getStatus()).isEqualTo("PENDING");
    }
}
