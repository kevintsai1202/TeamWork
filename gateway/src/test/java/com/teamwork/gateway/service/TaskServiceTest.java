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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRecordRepository taskRecordRepository;

    @Mock
    private IdentityPolicyProperties identityPolicyProperties;

    @Mock
    private AgentProfileRepository agentProfileRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AccountToolPermissionRepository accountToolPermissionRepository;

    @Mock
    private ToolConfigRepository toolConfigRepository;

    @Mock
    private MasterAgent masterAgent;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createNewTask_ShouldSaveAndReturnTaskRecord() {
        // Arrange
        TaskRequest request = new TaskRequest();
        request.setUserId("user-test-1");
        request.setProfileId("profile-test-1");
        request.setParentTaskId("");
        request.setInputPayload("Test payload");

        AccountToolPermission permission = new AccountToolPermission();
        permission.setUserId("user-test-1");
        permission.setToolId("tool-1");

        when(identityPolicyProperties.isAllowRequestUserId()).thenReturn(true);
        when(userAccountRepository.existsByIdAndStatus("user-test-1", "ACTIVE")).thenReturn(true);
        when(agentProfileRepository.existsById("profile-test-1")).thenReturn(true);
        when(accountToolPermissionRepository.findByUserId("user-test-1")).thenReturn(List.of(permission));
        when(toolConfigRepository.countByIdIn(List.of("tool-1"))).thenReturn(1L);

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

    @Test
    void createNewTask_ShouldThrowWhenUserIdMissing() {
        TaskRequest request = new TaskRequest();
        request.setProfileId("profile-test-1");
        request.setInputPayload("Test payload");

        when(identityPolicyProperties.isAllowRequestUserId()).thenReturn(true);

        assertThatThrownBy(() -> taskService.createNewTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId is required");

        verify(taskRecordRepository, never()).save(any(TaskRecord.class));
    }

    @Test
    void createNewTask_ShouldThrowWhenProfileNotFound() {
        TaskRequest request = new TaskRequest();
        request.setUserId("user-test-1");
        request.setProfileId("profile-missing");
        request.setInputPayload("Test payload");

        when(identityPolicyProperties.isAllowRequestUserId()).thenReturn(true);
        when(userAccountRepository.existsByIdAndStatus("user-test-1", "ACTIVE")).thenReturn(true);
        when(agentProfileRepository.existsById("profile-missing")).thenReturn(false);

        assertThatThrownBy(() -> taskService.createNewTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent profile not found");

        verify(taskRecordRepository, never()).save(any(TaskRecord.class));
    }

    @Test
    void createNewTask_ShouldThrowWhenUserNotActive() {
        TaskRequest request = new TaskRequest();
        request.setUserId("user-disabled");
        request.setProfileId("profile-test-1");
        request.setInputPayload("Test payload");

        when(identityPolicyProperties.isAllowRequestUserId()).thenReturn(true);
        when(userAccountRepository.existsByIdAndStatus("user-disabled", "ACTIVE")).thenReturn(false);

        assertThatThrownBy(() -> taskService.createNewTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User does not exist or is not active");

        verify(taskRecordRepository, never()).save(any(TaskRecord.class));
    }

    @Test
    void createNewTask_ShouldThrowWhenNoToolPermission() {
        TaskRequest request = new TaskRequest();
        request.setUserId("user-no-tool");
        request.setProfileId("profile-test-1");
        request.setInputPayload("Test payload");

        when(identityPolicyProperties.isAllowRequestUserId()).thenReturn(true);
        when(userAccountRepository.existsByIdAndStatus("user-no-tool", "ACTIVE")).thenReturn(true);
        when(agentProfileRepository.existsById("profile-test-1")).thenReturn(true);
        when(accountToolPermissionRepository.findByUserId("user-no-tool")).thenReturn(List.of());

        assertThatThrownBy(() -> taskService.createNewTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User has no authorized tools");

        verify(taskRecordRepository, never()).save(any(TaskRecord.class));
    }

    @Test
    void createNewTask_ShouldThrowWhenRequestUserIdIsDisabled() {
        TaskRequest request = new TaskRequest();
        request.setUserId("user-test-1");
        request.setProfileId("profile-test-1");
        request.setInputPayload("Test payload");

        when(identityPolicyProperties.isAllowRequestUserId()).thenReturn(false);

        assertThatThrownBy(() -> taskService.createNewTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId in request body is disabled in this environment");

        verify(taskRecordRepository, never()).save(any(TaskRecord.class));
    }
}
