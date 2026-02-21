package com.teamwork.gateway.service;

import com.teamwork.gateway.agent.MasterAgent;
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
    private final MasterAgent masterAgent;

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

        // 觸發 MasterAgent 進行非同步任務處理
        masterAgent.processTask(savedTask.getId(), request.getInputPayload());

        return savedTask;
    }
}
