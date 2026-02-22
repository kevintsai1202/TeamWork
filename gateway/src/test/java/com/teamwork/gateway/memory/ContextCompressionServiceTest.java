package com.teamwork.gateway.memory;

import org.springframework.context.ApplicationEventPublisher;
import com.teamwork.gateway.event.ContextCompressedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContextCompressionServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ContextCompressionService contextCompressionService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        contextCompressionService = new ContextCompressionService(redisTemplate, eventPublisher, true, 10, 5, 1);
    }

    @Test
    void compressIfNeeded_WhenTokenBelowThreshold_ShouldDoNothing() {
        String key = "chat:memory:task-1";
        when(listOperations.size(key)).thenReturn(1L);
        when(listOperations.range(key, 0, 0)).thenReturn(List.of("{\"type\":\"USER\",\"content\":\"hi\"}"));

        contextCompressionService.compressIfNeeded("task-1");

        verify(redisTemplate, never()).delete(key);
        verify(valueOperations, never()).increment(anyString());
        ContextCompressionMetricsSnapshot snapshot = contextCompressionService.getMetricsSnapshot();
        assertThat(snapshot.attempts()).isEqualTo(1);
        assertThat(snapshot.compressed()).isEqualTo(0);
    }

    @Test
    void compressIfNeeded_WhenTokenOverThreshold_ShouldReplaceWithSummaryAndRetained() {
        String key = "chat:memory:task-2";
        when(listOperations.size(key)).thenReturn(3L);
        when(listOperations.range(key, 0, 2)).thenReturn(List.of(
                "{\"type\":\"USER\",\"content\":\"12345678901234567890\"}",
                "{\"type\":\"ASSISTANT\",\"content\":\"abcdefghijabcdefghij\"}",
                "{\"type\":\"USER\",\"content\":\"retain-me\"}"));

        contextCompressionService.compressIfNeeded("task-2");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).delete(key);
        verify(listOperations).rightPushAll(eq(key), captor.capture());
        verify(valueOperations).increment("chat:memory:compression-count:task-2");
        verify(eventPublisher).publishEvent(any(ContextCompressedEvent.class));

        List<String> stored = captor.getValue();
        assertThat(stored).hasSize(2);
        assertThat(stored.get(0)).contains("\"type\":\"SYSTEM\"").contains("AUTO_COMPRESSED");
        assertThat(stored.get(0)).contains("1. 目標與範圍").contains("5. 下一步");
        assertThat(stored.get(1)).contains("retain-me");

        ContextCompressionMetricsSnapshot snapshot = contextCompressionService.getMetricsSnapshot();
        assertThat(snapshot.attempts()).isEqualTo(1);
        assertThat(snapshot.compressed()).isEqualTo(1);
        assertThat(snapshot.failures()).isEqualTo(0);
        assertThat(snapshot.totalSavedTokens()).isGreaterThanOrEqualTo(0);
        assertThat(snapshot.averageSavedRatio()).isBetween(0.0, 1.0);
    }

    @Test
    void getCompressionCount_WhenMissingOrInvalid_ShouldReturnZero() {
        when(valueOperations.get("chat:memory:compression-count:task-3")).thenReturn(null);
        assertThat(contextCompressionService.getCompressionCount("task-3")).isEqualTo(0L);

        when(valueOperations.get("chat:memory:compression-count:task-3")).thenReturn("abc");
        assertThat(contextCompressionService.getCompressionCount("task-3")).isEqualTo(0L);
    }

    @Test
    void getMetricsSnapshot_WhenCompressionThrows_ShouldIncreaseFailureRate() {
        String key = "chat:memory:task-failure";
        when(listOperations.size(key)).thenReturn(2L);
        when(listOperations.range(key, 0, 1)).thenThrow(new RuntimeException("redis-io"));

        try {
            contextCompressionService.compressIfNeeded("task-failure");
        } catch (RuntimeException ignored) {
        }

        ContextCompressionMetricsSnapshot snapshot = contextCompressionService.getMetricsSnapshot();
        assertThat(snapshot.attempts()).isEqualTo(1);
        assertThat(snapshot.failures()).isEqualTo(1);
        assertThat(snapshot.failureRate()).isEqualTo(1.0);
    }
}
