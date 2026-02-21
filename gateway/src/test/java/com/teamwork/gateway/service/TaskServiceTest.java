package com.teamwork.gateway.service;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRecordRepository taskRecordRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createNewTask_ShouldSaveAndReturnTaskRecord() {
        // Arrange
        TaskRequest request = new TaskRequest();
        request.setProfileId("profile-test-1");
        request.setParentTaskId("");
        request.setInputPayload("test payload");

        TaskRecord savedEntity = new TaskRecord();
        savedEntity.setId(UUID.randomUUID().toString());
        savedEntity.setProfileId(request.getProfileId());
        savedEntity.setParentTaskId(request.getParentTaskId());
        savedEntity.setInputPayload(request.getInputPayload());
        savedEntity.setStatus("PENDING");

        when(taskRecordRepository.save(any(TaskRecord.class))).thenReturn(savedEntity);

        // Act
        TaskRecord result = taskService.createNewTask(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedEntity.getId());
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getProfileId()).isEqualTo("profile-test-1");

        verify(taskRecordRepository).save(any(TaskRecord.class));
    }
}
