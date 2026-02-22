package com.teamwork.gateway.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate redisTemplate;
    private final ContextCompressionService contextCompressionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisChatMemory(StringRedisTemplate redisTemplate, ContextCompressionService contextCompressionService) {
        this.redisTemplate = redisTemplate;
        this.contextCompressionService = contextCompressionService;
    }

    private static final String KEY_PREFIX = "chat:memory:";

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        List<String> jsonMessages = messages.stream()
                .map(ChatMessageDto::fromMessage)
                .map(this::toJson)
                .filter(Objects::nonNull)
                .toList();

        if (!jsonMessages.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(key, jsonMessages);
            contextCompressionService.compressIfNeeded(conversationId);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        return getLastN(conversationId, Integer.MAX_VALUE);
    }

    public List<Message> get(String conversationId, int lastN) {
        return getLastN(conversationId, lastN);
    }

    public List<Message> getLastN(String conversationId, int lastN) {
        String key = KEY_PREFIX + conversationId;
        long size = redisTemplate.opsForList().size(key) != null ? redisTemplate.opsForList().size(key) : 0;

        if (size == 0) {
            return Collections.emptyList();
        }

        long start = Math.max(0, size - lastN);
        List<String> jsonMessages = redisTemplate.opsForList().range(key, start, size - 1);

        if (jsonMessages == null) {
            return Collections.emptyList();
        }

        return jsonMessages.stream()
                .map(this::fromJson)
                .filter(Objects::nonNull)
                .map(ChatMessageDto::toMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
        contextCompressionService.clearCompressionCount(conversationId);
    }

    private String toJson(ChatMessageDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ChatMessageDto", e);
            return null;
        }
    }

    private ChatMessageDto fromJson(String json) {
        try {
            return objectMapper.readValue(json, ChatMessageDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ChatMessageDto", e);
            return null;
        }
    }
}
