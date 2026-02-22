package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.AgentContextDetailResponse;
import com.teamwork.gateway.dto.AgentContextUsageItem;
import com.teamwork.gateway.dto.ContextMessageItem;
import com.teamwork.gateway.memory.ContextCompressionService;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.memory.RedisChatMemory;
import com.teamwork.gateway.repository.TaskRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContextQueryService {

    private static final String DEFAULT_AGENT_NAME = "master-agent";

    private final TaskRecordRepository taskRecordRepository;
    private final RedisChatMemory redisChatMemory;
    private final ContextCompressionService contextCompressionService;

    /**
     * 查詢上下文用量統計，支援 agentName/taskId/status 篩選。
     */
    public List<AgentContextUsageItem> findUsage(String agentName, String taskId, String status) {
        if (isSpecified(agentName) && !DEFAULT_AGENT_NAME.equalsIgnoreCase(agentName)) {
            return Collections.emptyList();
        }

        List<TaskRecord> taskRecords = resolveTaskRecords(taskId, status);
        return taskRecords.stream()
                .map(this::toUsageItem)
                .toList();
    }

    /**
     * 取得指定 taskId 的完整上下文內容。
     */
    public AgentContextDetailResponse getFullContext(String taskId) {
        TaskRecord taskRecord = taskRecordRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        List<Message> messages = redisChatMemory.get(taskId);
        List<ContextMessageItem> messageItems = new ArrayList<>(messages.size());
        for (int index = 0; index < messages.size(); index++) {
            Message message = messages.get(index);
            messageItems.add(new ContextMessageItem(
                    index + 1,
                    resolveRole(message),
                    Optional.ofNullable(message.getText()).orElse(""),
                    estimateTokens(message.getText())));
        }

        return new AgentContextDetailResponse(
                taskId,
                DEFAULT_AGENT_NAME,
                "",
                messageItems,
                List.of(),
                List.of());
    }

    private List<TaskRecord> resolveTaskRecords(String taskId, String status) {
        if (isSpecified(taskId)) {
            return taskRecordRepository.findById(taskId).stream().toList();
        }

        if (isSpecified(status)) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            return taskRecordRepository.findAll().stream()
                    .filter(record -> normalizedStatus.equalsIgnoreCase(record.getStatus()))
                    .toList();
        }

        return taskRecordRepository.findAll();
    }

    private AgentContextUsageItem toUsageItem(TaskRecord taskRecord) {
        List<Message> messages = redisChatMemory.get(taskRecord.getId());
        long estimatedTokens = messages.stream()
                .map(Message::getText)
                .mapToLong(this::estimateTokens)
                .sum();

        LocalDateTime lastUpdatedAt = taskRecord.getUpdatedAt() != null
                ? taskRecord.getUpdatedAt()
                : taskRecord.getCreatedAt();

        return new AgentContextUsageItem(
                DEFAULT_AGENT_NAME,
                taskRecord.getId(),
                messages.size(),
                estimatedTokens,
            contextCompressionService.getCompressionCount(taskRecord.getId()),
                lastUpdatedAt);
    }

    private String resolveRole(Message message) {
        if (message instanceof SystemMessage) {
            return "system";
        }
        if (message instanceof AssistantMessage) {
            return "assistant";
        }
        return "user";
    }

    private long estimateTokens(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return Math.max(1, (content.length() + 3L) / 4L);
    }

    private boolean isSpecified(String value) {
        return value != null && !value.isBlank();
    }
}
