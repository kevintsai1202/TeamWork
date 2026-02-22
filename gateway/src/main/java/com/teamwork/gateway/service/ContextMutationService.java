package com.teamwork.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamwork.gateway.dto.ContextDeleteRequest;
import com.teamwork.gateway.dto.ContextDeleteResponse;
import com.teamwork.gateway.entity.ContextDeletionAudit;
import com.teamwork.gateway.memory.ChatMessageDto;
import com.teamwork.gateway.repository.ContextDeletionAuditRepository;
import com.teamwork.gateway.repository.TaskRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContextMutationService {

    private static final String CHAT_KEY_PREFIX = "chat:memory:";
    private static final String OPERATOR_ID_SYSTEM = "system";

    private final TaskRecordRepository taskRecordRepository;
    private final ContextDeletionAuditRepository contextDeletionAuditRepository;
    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContextDeleteResponse deleteContext(String taskId, ContextDeleteRequest request) {
        taskRecordRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (request == null || request.mode() == null || request.mode().isBlank()) {
            throw new IllegalArgumentException("Delete mode is required");
        }

        String mode = request.mode().trim().toUpperCase(Locale.ROOT);
        String key = CHAT_KEY_PREFIX + taskId;
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size <= 0) {
            ContextDeletionAudit audit = saveAudit(taskId, mode, 0, null, null, request.reason());
            return new ContextDeleteResponse(taskId, false, 0, audit.getId());
        }

        List<String> rawMessages = redisTemplate.opsForList().range(key, 0, size - 1);
        if (rawMessages == null || rawMessages.isEmpty()) {
            ContextDeletionAudit audit = saveAudit(taskId, mode, 0, null, null, request.reason());
            return new ContextDeleteResponse(taskId, false, 0, audit.getId());
        }

        List<ChatMessageDto> messages = rawMessages.stream()
                .map(this::fromJson)
                .filter(Objects::nonNull)
                .toList();

        DeletePlan deletePlan = buildDeletePlan(mode, request, messages);
        List<ChatMessageDto> retained = new ArrayList<>();
        for (int index = 0; index < messages.size(); index++) {
            if (!deletePlan.deleteIndexes().contains(index)) {
                retained.add(messages.get(index));
            }
        }

        redisTemplate.delete(key);
        List<String> retainedJson = retained.stream()
                .map(this::toJson)
                .filter(Objects::nonNull)
                .toList();
        if (!retainedJson.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(key, retainedJson);
        }

        ContextDeletionAudit audit = saveAudit(
                taskId,
                mode,
                deletePlan.removedCount(),
                deletePlan.fromIndex(),
                deletePlan.toIndex(),
                request.reason());

        return new ContextDeleteResponse(taskId, deletePlan.removedCount() > 0, deletePlan.removedCount(), audit.getId());
    }

    private DeletePlan buildDeletePlan(String mode, ContextDeleteRequest request, List<ChatMessageDto> messages) {
        return switch (mode) {
            case "ALL_HISTORY" -> buildAllHistoryDeletePlan(messages.size());
            case "SUMMARY" -> buildSummaryDeletePlan(messages);
            case "SINGLE_MESSAGE" -> buildSingleMessageDeletePlan(request, messages.size());
            case "RANGE" -> buildRangeDeletePlan(request, messages.size());
            default -> throw new IllegalArgumentException("Unsupported delete mode: " + mode);
        };
    }

    private DeletePlan buildAllHistoryDeletePlan(int size) {
        List<Integer> deleteIndexes = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            deleteIndexes.add(index);
        }
        return new DeletePlan(deleteIndexes, size, 1, size);
    }

    private DeletePlan buildSummaryDeletePlan(List<ChatMessageDto> messages) {
        List<Integer> deleteIndexes = new ArrayList<>();
        for (int index = 0; index < messages.size(); index++) {
            ChatMessageDto dto = messages.get(index);
            boolean isSystem = "SYSTEM".equalsIgnoreCase(dto.getType());
            boolean isCompressedSummary = dto.getContent() != null && dto.getContent().contains("[AUTO_COMPRESSED]");
            if (isSystem && isCompressedSummary) {
                deleteIndexes.add(index);
            }
        }
        Integer from = deleteIndexes.isEmpty() ? null : deleteIndexes.getFirst() + 1;
        Integer to = deleteIndexes.isEmpty() ? null : deleteIndexes.getLast() + 1;
        return new DeletePlan(deleteIndexes, deleteIndexes.size(), from, to);
    }

    private DeletePlan buildSingleMessageDeletePlan(ContextDeleteRequest request, int size) {
        Integer index = extractFromIndex(request);
        validateIndex(index, size);
        int zeroBased = index - 1;
        return new DeletePlan(List.of(zeroBased), 1, index, index);
    }

    private DeletePlan buildRangeDeletePlan(ContextDeleteRequest request, int size) {
        Integer from = extractFromIndex(request);
        Integer to = extractToIndex(request);
        validateIndex(from, size);
        validateIndex(to, size);
        if (from > to) {
            throw new IllegalArgumentException("range.fromIndex must be <= range.toIndex");
        }
        List<Integer> deleteIndexes = new ArrayList<>();
        for (int index = from - 1; index <= to - 1; index++) {
            deleteIndexes.add(index);
        }
        return new DeletePlan(deleteIndexes, deleteIndexes.size(), from, to);
    }

    private Integer extractFromIndex(ContextDeleteRequest request) {
        if (request.range() == null || request.range().fromIndex() == null) {
            throw new IllegalArgumentException("range.fromIndex is required");
        }
        return request.range().fromIndex();
    }

    private Integer extractToIndex(ContextDeleteRequest request) {
        if (request.range() == null || request.range().toIndex() == null) {
            throw new IllegalArgumentException("range.toIndex is required");
        }
        return request.range().toIndex();
    }

    private void validateIndex(int index, int size) {
        if (index < 1 || index > size) {
            throw new IllegalArgumentException("index out of range: " + index);
        }
    }

    private ContextDeletionAudit saveAudit(
            String taskId,
            String mode,
            int removedCount,
            Integer fromIndex,
            Integer toIndex,
            String reason) {
        ContextDeletionAudit audit = new ContextDeletionAudit();
        audit.setTaskId(taskId);
        audit.setMode(mode);
        audit.setRemovedCount(removedCount);
        audit.setFromIndex(fromIndex);
        audit.setToIndex(toIndex);
        audit.setReason(reason);
        audit.setOperatorId(OPERATOR_ID_SYSTEM);
        return contextDeletionAuditRepository.save(audit);
    }

    private String toJson(ChatMessageDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private ChatMessageDto fromJson(String json) {
        try {
            return objectMapper.readValue(json, ChatMessageDto.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private record DeletePlan(List<Integer> deleteIndexes, int removedCount, Integer fromIndex, Integer toIndex) {
    }
}
