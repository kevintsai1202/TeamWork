package com.teamwork.gateway.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisChatMemoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ContextCompressionService contextCompressionService;

    @InjectMocks
    private RedisChatMemory redisChatMemory;

    private static final String CONVERSATION_ID = "test-chat-123";
    private static final String REDIS_KEY = "chat:memory:" + CONVERSATION_ID;

    @BeforeEach
    void setUp() {
        // leniency for the case get() does not need to push
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void add_GivenMessages_ShouldPushToRedisAsJson() {
        // Arrange
        Message userMessage = new UserMessage("Hello");
        Message aiMessage = new AssistantMessage("Hi there!");
        List<Message> messages = List.of(userMessage, aiMessage);

        // Act
        redisChatMemory.add(CONVERSATION_ID, messages);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(listOperations).rightPushAll(eq(REDIS_KEY), captor.capture());

        List<String> pushedJson = captor.getValue();
        assertThat(pushedJson).hasSize(2);
        assertThat(pushedJson.get(0)).contains("\"type\":\"USER\"").contains("\"content\":\"Hello\"");
        assertThat(pushedJson.get(1)).contains("\"type\":\"ASSISTANT\"").contains("\"content\":\"Hi there!\"");
        verify(contextCompressionService).compressIfNeeded(CONVERSATION_ID);
    }

    @Test
    void get_WhenMemoryIsEmpty_ShouldReturnEmptyList() {
        // Arrange
        when(listOperations.size(REDIS_KEY)).thenReturn(0L);

        // Act
        List<Message> result = redisChatMemory.get(CONVERSATION_ID, 10);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void get_WhenMemoryHasItems_ShouldReturnLastNMessages() {
        // Arrange
        int lastN = 2;
        long totalSize = 5L;
        when(listOperations.size(REDIS_KEY)).thenReturn(totalSize);

        List<String> mockJsons = List.of(
                "{\"type\":\"USER\",\"content\":\"Message 4\"}",
                "{\"type\":\"ASSISTANT\",\"content\":\"Message 5\"}");
        when(listOperations.range(REDIS_KEY, 3L, 4L)).thenReturn(mockJsons);

        // Act
        List<Message> result = redisChatMemory.get(CONVERSATION_ID, lastN);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isInstanceOf(UserMessage.class);
        assertThat(result.get(0).getText()).isEqualTo("Message 4");
        assertThat(result.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(result.get(1).getText()).isEqualTo("Message 5");
    }

    @Test
    void get_WhenDeserializationFails_ShouldFilterOutNulls() {
        // Arrange
        when(listOperations.size(REDIS_KEY)).thenReturn(1L);
        // Provide invalid JSON to force Jackson to throw an exception
        when(listOperations.range(REDIS_KEY, 0L, 0L)).thenReturn(Collections.singletonList("invalid json"));

        // Act
        List<Message> result = redisChatMemory.get(CONVERSATION_ID, 1);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void clear_ShouldDeleteRedisKey() {
        // Act
        redisChatMemory.clear(CONVERSATION_ID);

        // Assert
        verify(redisTemplate).delete(REDIS_KEY);
        verify(contextCompressionService).clearCompressionCount(CONVERSATION_ID);
    }
}
