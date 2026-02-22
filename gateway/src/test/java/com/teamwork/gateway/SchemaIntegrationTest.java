package com.teamwork.gateway;

import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.repository.TaskRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.teamwork.gateway.ai.ChatModelFactory;
import org.springframework.ai.chat.client.ChatClient;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:15432/teamwork",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.ai.openai.api-key=test-key"
})
class SchemaIntegrationTest {

    @Autowired
    private TaskRecordRepository taskRecordRepository;

    @MockitoBean
    private ChatModelFactory chatModelFactory;

    @MockitoBean
    private ChatClient.Builder chatClientBuilder;

    @Test
    void testSchemaIsCreatedAndEntityCanBeSaved() {
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
