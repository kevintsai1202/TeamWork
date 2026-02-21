package com.teamwork.gateway.service;

import com.teamwork.gateway.agent.MasterAgent;
import com.teamwork.gateway.dto.TaskRequest;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.repository.TaskRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRecordRepository taskRecordRepository;

    @Mock
    private MasterAgent masterAgent;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createNewTask_ShouldSaveAndReturnTaskRecord() {
        // Arrange
        TaskRequest request = new TaskRequest();
        request.setProfileId("profile-test-1");
        request.setParentTaskId("");
        request.setInputPayload("Test payload");

        TaskRecord savedEntity = new TaskRecord();
        savedEntity.setId("test-task-123");
        savedEntity.setProfileId(request.getProfileId());
        savedEntity.setParentTaskId(request.getParentTaskId());
        savedEntity.setInputPayload(request.getInputPayload());
        savedEntity.setStatus("PENDING");

        when(taskRecordRepository.save(any(TaskRecord.class))).thenReturn(savedEntity);

        // Act
        TaskRecord result = taskService.createNewTask(request);

        // Assert
        assertThat(result.getId()).isEqualTo("test-task-123");
        assertThat(result.getStatus()).isEqualTo("PENDING");

        // Verify that MasterAgent.processTask() was called
        verify(masterAgent).processTask(eq("test-task-123"), eq("Test payload"));
        verify(taskRecordRepository).save(any(TaskRecord.class));
    }
}
