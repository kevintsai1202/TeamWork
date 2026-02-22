package com.teamwork.gateway.service;

import com.teamwork.gateway.agent.MasterAgent;
import com.teamwork.gateway.config.IdentityPolicyProperties;
import com.teamwork.gateway.dto.TaskRequest;
import com.teamwork.gateway.entity.AccountToolPermission;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.repository.AccountToolPermissionRepository;
import com.teamwork.gateway.repository.AgentProfileRepository;
import com.teamwork.gateway.repository.TaskRecordRepository;
import com.teamwork.gateway.repository.ToolConfigRepository;
import com.teamwork.gateway.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final String USER_STATUS_ACTIVE = "ACTIVE";

    private final TaskRecordRepository taskRecordRepository;
    private final IdentityPolicyProperties identityPolicyProperties;
    private final AgentProfileRepository agentProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final AccountToolPermissionRepository accountToolPermissionRepository;
    private final ToolConfigRepository toolConfigRepository;
    private final MasterAgent masterAgent;

    /**
     * 建立新任務並觸發非同步執行。
     */
    @Transactional
    public TaskRecord createNewTask(TaskRequest request) {
        validateGatewayRequest(request);

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

    /**
     * Gateway 驗證：請求參數、使用者存在、Profile 存在、RBAC 工具授權。
     */
    private void validateGatewayRequest(TaskRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Task request cannot be null");
        }

        if (!identityPolicyProperties.isAllowRequestUserId()) {
            throw new IllegalArgumentException("userId in request body is disabled in this environment");
        }

        if (isBlank(request.getUserId())) {
            throw new IllegalArgumentException("userId is required");
        }

        if (isBlank(request.getProfileId())) {
            throw new IllegalArgumentException("profileId is required");
        }

        boolean userExists = userAccountRepository.existsByIdAndStatus(request.getUserId(), USER_STATUS_ACTIVE);
        if (!userExists) {
            throw new IllegalArgumentException("User does not exist or is not active");
        }

        boolean profileExists = agentProfileRepository.existsById(request.getProfileId());
        if (!profileExists) {
            throw new IllegalArgumentException("Agent profile not found: " + request.getProfileId());
        }

        List<String> userToolIds = accountToolPermissionRepository.findByUserId(request.getUserId())
                .stream()
                .map(AccountToolPermission::getToolId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();

        if (userToolIds.isEmpty()) {
            throw new IllegalArgumentException("User has no authorized tools");
        }

        long existingToolCount = toolConfigRepository.countByIdIn(userToolIds);
        if (existingToolCount <= 0) {
            throw new IllegalArgumentException("User authorized tools are not registered in tool_configs");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
