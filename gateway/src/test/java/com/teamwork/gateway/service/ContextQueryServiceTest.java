package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.AgentContextDetailResponse;
import com.teamwork.gateway.dto.AgentContextUsageItem;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.memory.ContextCompressionService;
import com.teamwork.gateway.memory.RedisChatMemory;
import com.teamwork.gateway.repository.TaskRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContextQueryServiceTest {

    @Mock
    private TaskRecordRepository taskRecordRepository;

    @Mock
    private RedisChatMemory redisChatMemory;

    @Mock
    private ContextCompressionService contextCompressionService;

    @InjectMocks
    private ContextQueryService contextQueryService;

    @Test
    void findUsage_WhenAgentNameIsNotSupported_ShouldReturnEmpty() {
        List<AgentContextUsageItem> result = contextQueryService.findUsage("other-agent", null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void findUsage_WhenTaskExists_ShouldReturnUsageWithEstimatedTokens() {
        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setId("task-1");
        taskRecord.setStatus("COMPLETED");
        taskRecord.setUpdatedAt(LocalDateTime.of(2026, 2, 23, 12, 0, 0));

        List<Message> messages = List.of(new UserMessage("hello world"), new AssistantMessage("ok"));

        given(taskRecordRepository.findById("task-1")).willReturn(Optional.of(taskRecord));
        given(redisChatMemory.get("task-1")).willReturn(messages);
        given(contextCompressionService.getCompressionCount("task-1")).willReturn(3L);

        List<AgentContextUsageItem> result = contextQueryService.findUsage("master-agent", "task-1", null);

        assertThat(result).hasSize(1);
        AgentContextUsageItem item = result.getFirst();
        assertThat(item.agentName()).isEqualTo("master-agent");
        assertThat(item.taskId()).isEqualTo("task-1");
        assertThat(item.messageCount()).isEqualTo(2);
        assertThat(item.estimatedTokens()).isEqualTo(4);
        assertThat(item.compressionCount()).isEqualTo(3);
        assertThat(item.lastUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 2, 23, 12, 0, 0));
    }

    @Test
    void getFullContext_WhenTaskNotFound_ShouldThrowIllegalArgumentException() {
        given(taskRecordRepository.findById("missing-task")).willReturn(Optional.empty());

        assertThatThrownBy(() -> contextQueryService.getFullContext("missing-task"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task not found: missing-task");
    }

    @Test
    void getFullContext_WhenTaskExists_ShouldReturnRoleMappedMessages() {
        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setId("task-2");

        List<Message> messages = List.of(
                new SystemMessage("sys"),
                new UserMessage("hello"),
                new AssistantMessage("done"));

        given(taskRecordRepository.findById("task-2")).willReturn(Optional.of(taskRecord));
        given(redisChatMemory.get("task-2")).willReturn(messages);

        AgentContextDetailResponse response = contextQueryService.getFullContext("task-2");

        assertThat(response.taskId()).isEqualTo("task-2");
        assertThat(response.agentName()).isEqualTo("master-agent");
        assertThat(response.messages()).hasSize(3);
        assertThat(response.messages().get(0).role()).isEqualTo("system");
        assertThat(response.messages().get(1).role()).isEqualTo("user");
        assertThat(response.messages().get(2).role()).isEqualTo("assistant");
        assertThat(response.compressedSummaries()).isEmpty();
        assertThat(response.toolCalls()).isEmpty();
    }
}
