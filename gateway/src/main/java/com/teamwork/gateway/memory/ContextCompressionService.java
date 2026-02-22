package com.teamwork.gateway.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamwork.gateway.event.ContextCompressedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.concurrent.atomic.LongAdder;

@Service
@Slf4j
public class ContextCompressionService {

    private static final String CHAT_KEY_PREFIX = "chat:memory:";
    private static final String COMPRESSION_COUNT_KEY_PREFIX = "chat:memory:compression-count:";
        private static final String TEMPLATE_STAGE_A = """
            [STAGE_A]
            你是 Context Compressor。請抽取不可遺失資訊：
            {facts, decisions, constraints, open_questions, errors_and_fixes, pending_todos}
            規則：僅保留會影響後續執行內容，不可杜撰。
            """;
        private static final String TEMPLATE_STAGE_B = """
            [STAGE_B]
            根據 Stage A 產生可續跑摘要，固定輸出：
            1. 目標與範圍
            2. 已完成
            3. 進行中
            4. 風險與限制
            5. 下一步
            """;

    private final StringRedisTemplate redisTemplate;
        private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final boolean enabled;
    private final long thresholdTokens;
    private final long targetTokens;
    private final int retainRecentMessages;
        private final LongAdder compressionAttempts = new LongAdder();
        private final LongAdder compressionSuccess = new LongAdder();
        private final LongAdder compressionFailures = new LongAdder();
        private final LongAdder totalSavedTokens = new LongAdder();
        private final LongAdder totalSavedRatioPermille = new LongAdder();

    public ContextCompressionService(
            StringRedisTemplate redisTemplate,
            ApplicationEventPublisher eventPublisher,
            @Value("${gateway.context-compression.enabled:true}") boolean enabled,
            @Value("${gateway.context-compression.threshold-tokens:12000}") long thresholdTokens,
            @Value("${gateway.context-compression.target-tokens:6000}") long targetTokens,
            @Value("${gateway.context-compression.retain-recent-messages:8}") int retainRecentMessages) {
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.enabled = enabled;
        this.thresholdTokens = thresholdTokens;
        this.targetTokens = targetTokens;
        this.retainRecentMessages = Math.max(0, retainRecentMessages);
    }

    /**
     * 若上下文 token 估算超過閾值，將舊歷史壓縮為一筆 system summary。
     */
    public void compressIfNeeded(String conversationId) {
        if (!enabled) {
            return;
        }
        compressionAttempts.increment();

        try {
            String chatKey = CHAT_KEY_PREFIX + conversationId;
            Long size = redisTemplate.opsForList().size(chatKey);
            if (size == null || size <= 0) {
                return;
            }

            List<String> rawMessages = redisTemplate.opsForList().range(chatKey, 0, size - 1);
            if (rawMessages == null || rawMessages.isEmpty()) {
                return;
            }

            List<ChatMessageDto> messageDtos = rawMessages.stream()
                    .map(this::fromJson)
                    .filter(Objects::nonNull)
                    .toList();
            if (messageDtos.isEmpty()) {
                return;
            }

            long estimatedTokens = messageDtos.stream()
                    .map(ChatMessageDto::getContent)
                    .mapToLong(this::estimateTokens)
                    .sum();
            if (estimatedTokens <= thresholdTokens) {
                return;
            }

            int splitIndex = Math.max(0, messageDtos.size() - retainRecentMessages);
            List<ChatMessageDto> toCompress = messageDtos.subList(0, splitIndex);
            List<ChatMessageDto> retained = messageDtos.subList(splitIndex, messageDtos.size());

            ChatMessageDto summaryMessage = buildSummaryMessage(conversationId, toCompress, estimatedTokens);
            List<ChatMessageDto> merged = new ArrayList<>();
            merged.add(summaryMessage);
            merged.addAll(retained);

            List<String> jsonMessages = merged.stream()
                    .map(this::toJson)
                    .filter(Objects::nonNull)
                    .toList();

            redisTemplate.delete(chatKey);
            if (!jsonMessages.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(chatKey, jsonMessages);
            }

            redisTemplate.opsForValue().increment(COMPRESSION_COUNT_KEY_PREFIX + conversationId);

            long afterTokens = merged.stream()
                    .map(ChatMessageDto::getContent)
                    .mapToLong(this::estimateTokens)
                    .sum();
            long savedTokens = Math.max(0L, estimatedTokens - afterTokens);
            double savedRatio = estimatedTokens == 0 ? 0.0 : (double) savedTokens / (double) estimatedTokens;
            compressionSuccess.increment();
            totalSavedTokens.add(savedTokens);
            totalSavedRatioPermille.add(Math.round(savedRatio * 1000));

            publishContextCompressedEvent(conversationId, estimatedTokens, afterTokens, savedTokens, savedRatio);

            log.info("Context compressed. conversationId={}, beforeMessages={}, afterMessages={}, beforeTokens={}, afterTokens={}, savedTokens={}, savedRatio={}, targetTokens={}",
                    conversationId, messageDtos.size(), merged.size(), estimatedTokens, afterTokens, savedTokens, savedRatio, targetTokens);
        } catch (RuntimeException ex) {
            compressionFailures.increment();
            throw ex;
        }
    }

    public ContextCompressionMetricsSnapshot getMetricsSnapshot() {
        long attempts = compressionAttempts.sum();
        long compressed = compressionSuccess.sum();
        long failures = compressionFailures.sum();
        double failureRate = attempts == 0 ? 0.0 : (double) failures / (double) attempts;
        double averageSavedRatio = compressed == 0
                ? 0.0
                : ((double) totalSavedRatioPermille.sum() / 1000.0) / (double) compressed;
        return new ContextCompressionMetricsSnapshot(
                attempts,
                compressed,
                failures,
                totalSavedTokens.sum(),
                failureRate,
                averageSavedRatio);
    }

    /**
     * 取得該對話歷史累積壓縮次數。
     */
    public long getCompressionCount(String conversationId) {
        String value = redisTemplate.opsForValue().get(COMPRESSION_COUNT_KEY_PREFIX + conversationId);
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public void clearCompressionCount(String conversationId) {
        redisTemplate.delete(COMPRESSION_COUNT_KEY_PREFIX + conversationId);
    }

    private ChatMessageDto buildSummaryMessage(String conversationId, List<ChatMessageDto> compressedMessages, long beforeTokens) {
        CompressionExtraction extraction = extractKeyInformation(compressedMessages);
        String summary = buildStructuredSummary(conversationId, extraction, compressedMessages.size(), beforeTokens);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("compressed", true);
        metadata.put("sourceMessageCount", compressedMessages.size());
        metadata.put("beforeTokens", beforeTokens);
        metadata.put("targetTokens", targetTokens);
        metadata.put("templateStageA", TEMPLATE_STAGE_A);
        metadata.put("templateStageB", TEMPLATE_STAGE_B);

        return ChatMessageDto.builder()
                .type("SYSTEM")
                .content(summary)
                .metadata(metadata)
                .build();
    }

    private long estimateTokens(String content) {
        if (content == null || content.isBlank()) {
            return 0L;
        }
        return Math.max(1, (content.length() + 3L) / 4L);
    }

    private String toJson(ChatMessageDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ChatMessageDto in compression", e);
            return null;
        }
    }

    private ChatMessageDto fromJson(String json) {
        try {
            return objectMapper.readValue(json, ChatMessageDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize context message for compression");
            return null;
        }
    }

    private void publishContextCompressedEvent(
            String taskId,
            long beforeTokens,
            long afterTokens,
            long savedTokens,
            double savedRatio) {
        if (eventPublisher == null) {
            return;
        }
        eventPublisher.publishEvent(new ContextCompressedEvent(
                this,
                taskId,
                beforeTokens,
                afterTokens,
                savedTokens,
                savedRatio));
    }

    private CompressionExtraction extractKeyInformation(List<ChatMessageDto> messages) {
        Map<String, List<String>> bucket = new LinkedHashMap<>();
        bucket.put("facts", new ArrayList<>());
        bucket.put("decisions", new ArrayList<>());
        bucket.put("constraints", new ArrayList<>());
        bucket.put("open_questions", new ArrayList<>());
        bucket.put("errors_and_fixes", new ArrayList<>());
        bucket.put("pending_todos", new ArrayList<>());

        for (ChatMessageDto message : messages) {
            if (message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            String[] lines = message.getContent().split("\\r?\\n");
            for (String rawLine : lines) {
                String line = rawLine == null ? "" : rawLine.trim();
                if (line.isBlank()) {
                    continue;
                }
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.contains("todo") || lower.contains("待辦") || lower.contains("next")) {
                    addLimited(bucket.get("pending_todos"), line, 4);
                } else if (lower.contains("error") || lower.contains("exception") || lower.contains("修正") || lower.contains("fix")) {
                    addLimited(bucket.get("errors_and_fixes"), line, 4);
                } else if (lower.contains("?") || lower.contains("請問") || lower.contains("是否")) {
                    addLimited(bucket.get("open_questions"), line, 4);
                } else if (lower.contains("must") || lower.contains("限制") || lower.contains("required") || lower.contains("不可")) {
                    addLimited(bucket.get("constraints"), line, 4);
                } else if (lower.contains("決定") || lower.contains("adopt") || lower.contains("choose") || lower.contains("採用")) {
                    addLimited(bucket.get("decisions"), line, 4);
                } else {
                    addLimited(bucket.get("facts"), line, 6);
                }
            }
        }
        return new CompressionExtraction(
                bucket.get("facts"),
                bucket.get("decisions"),
                bucket.get("constraints"),
                bucket.get("open_questions"),
                bucket.get("errors_and_fixes"),
                bucket.get("pending_todos"));
    }

    private String buildStructuredSummary(
            String conversationId,
            CompressionExtraction extraction,
            int sourceMessages,
            long beforeTokens) {
        StringBuilder builder = new StringBuilder();
        builder.append("[AUTO_COMPRESSED]")
                .append(" conversationId=").append(conversationId)
                .append(", sourceMessages=").append(sourceMessages)
                .append(", beforeTokens=").append(beforeTokens)
                .append(", targetTokens=").append(targetTokens)
                .append(", at=").append(LocalDateTime.now())
                .append("\n")
                .append("stageA=facts=").append(extraction.facts().size())
                .append(", decisions=").append(extraction.decisions().size())
                .append(", constraints=").append(extraction.constraints().size())
                .append(", open_questions=").append(extraction.openQuestions().size())
                .append(", errors_and_fixes=").append(extraction.errorsAndFixes().size())
                .append(", pending_todos=").append(extraction.pendingTodos().size())
                .append("\n")
                .append("1. 目標與範圍\n")
                .append(formatList(extraction.facts(), "- 無可用目標資訊"))
                .append("2. 已完成\n")
                .append(formatList(extraction.decisions(), "- 無明確完成決策"))
                .append("3. 進行中\n")
                .append(formatList(extraction.pendingTodos(), "- 無待辦項目"))
                .append("4. 風險與限制\n")
                .append(formatList(extraction.constraints(), "- 無明確限制"))
                .append(formatList(extraction.errorsAndFixes(), "- 無錯誤修正記錄"))
                .append("5. 下一步\n")
                .append(formatList(extraction.openQuestions(), "- 依既有任務流程續行"));
        return builder.toString();
    }

    private String formatList(List<String> items, String fallback) {
        if (items == null || items.isEmpty()) {
            return fallback + "\n";
        }
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            builder.append("- ").append(item).append("\n");
        }
        return builder.toString();
    }

    private void addLimited(List<String> bucket, String value, int maxItems) {
        if (bucket.size() >= maxItems) {
            return;
        }
        if (!bucket.contains(value)) {
            bucket.add(value);
        }
    }

    private record CompressionExtraction(
            List<String> facts,
            List<String> decisions,
            List<String> constraints,
            List<String> openQuestions,
            List<String> errorsAndFixes,
            List<String> pendingTodos) {
    }
}
