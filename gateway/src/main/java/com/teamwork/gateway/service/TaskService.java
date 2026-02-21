package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.TaskRequest;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.repository.TaskRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRecordRepository taskRecordRepository;

    @Transactional
    public TaskRecord createNewTask(TaskRequest request) {
        log.info("Creating new task for profile: {}", request.getProfileId());

        TaskRecord task = new TaskRecord();
        task.setProfileId(request.getProfileId());
        task.setParentTaskId(request.getParentTaskId());
        task.setInputPayload(request.getInputPayload());
        task.setStatus("PENDING"); // 初始狀態

        TaskRecord savedTask = taskRecordRepository.save(task);
        log.info("Task created successfully with ID: {}", savedTask.getId());

        // 未來這裡可將 Task 丟入 Queue 或 @Async Event 進行處理

        return savedTask;
    }
}
