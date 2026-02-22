package com.teamwork.gateway.controller;

import com.teamwork.gateway.dto.TaskRequest;
import com.teamwork.gateway.dto.TaskResponse;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        TaskRecord task = taskService.createNewTask(request);

        TaskResponse response = new TaskResponse(
                task.getId(),
                task.getStatus(),
                "Task created successfully and is pending execution.");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
