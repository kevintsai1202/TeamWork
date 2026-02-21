package com.teamwork.gateway.controller;

import com.teamwork.gateway.dto.TaskRequest;
import com.teamwork.gateway.dto.TaskResponse;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    @Test
    void createTask_ShouldReturnAcceptedAndTaskResponse() {
        // Arrange
        TaskRequest request = new TaskRequest();
        request.setProfileId("profile-1");
        request.setInputPayload("{\"msg\":\"hello\"}");

        TaskRecord mockTask = new TaskRecord();
        mockTask.setId("task-uuid-1234");
        mockTask.setStatus("PENDING");

        given(taskService.createNewTask(any(TaskRequest.class))).willReturn(mockTask);

        // Act
        ResponseEntity<TaskResponse> response = taskController.createTask(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTaskId()).isEqualTo("task-uuid-1234");
        assertThat(response.getBody().getStatus()).isEqualTo("PENDING");
    }
}
