package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.ContextDeleteRange;
import com.teamwork.gateway.dto.ContextDeleteRequest;
import com.teamwork.gateway.dto.ContextDeleteResponse;
import com.teamwork.gateway.entity.ContextDeletionAudit;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.repository.ContextDeletionAuditRepository;
import com.teamwork.gateway.repository.TaskRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContextMutationServiceTest {

    @Mock
    private TaskRecordRepository taskRecordRepository;

    @Mock
    private ContextDeletionAuditRepository contextDeletionAuditRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private ContextMutationService contextMutationService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        ContextDeletionAudit audit = new ContextDeletionAudit();
        audit.setId("audit-1");
        when(contextDeletionAuditRepository.save(any(ContextDeletionAudit.class))).thenReturn(audit);
    }

    @Test
    void deleteContext_WhenTaskMissing_ShouldThrow() {
        given(taskRecordRepository.findById("task-x")).willReturn(Optional.empty());

        assertThatThrownBy(() -> contextMutationService.deleteContext("task-x", new ContextDeleteRequest("ALL_HISTORY", null, "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task not found: task-x");
    }

    @Test
    void deleteContext_AllHistory_ShouldRemoveAllAndReturnAudit() {
        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setId("task-1");
        given(taskRecordRepository.findById("task-1")).willReturn(Optional.of(taskRecord));

        String key = "chat:memory:task-1";
        when(listOperations.size(key)).thenReturn(2L);
        when(listOperations.range(key, 0, 1)).thenReturn(List.of(
                "{\"type\":\"USER\",\"content\":\"a\"}",
                "{\"type\":\"ASSISTANT\",\"content\":\"b\"}"));

        ContextDeleteResponse response = contextMutationService.deleteContext("task-1", new ContextDeleteRequest("ALL_HISTORY", null, "cleanup"));

        assertThat(response.removed()).isTrue();
        assertThat(response.removedCount()).isEqualTo(2);
        assertThat(response.auditId()).isEqualTo("audit-1");
        verify(redisTemplate).delete(key);
    }

    @Test
    void deleteContext_Range_ShouldRetainOutsideRange() {
        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setId("task-2");
        given(taskRecordRepository.findById("task-2")).willReturn(Optional.of(taskRecord));

        String key = "chat:memory:task-2";
        when(listOperations.size(key)).thenReturn(3L);
        when(listOperations.range(key, 0, 2)).thenReturn(List.of(
                "{\"type\":\"USER\",\"content\":\"m1\"}",
                "{\"type\":\"USER\",\"content\":\"m2\"}",
                "{\"type\":\"USER\",\"content\":\"m3\"}"));

        ContextDeleteRequest request = new ContextDeleteRequest("RANGE", new ContextDeleteRange(2, 3), "trim");
        ContextDeleteResponse response = contextMutationService.deleteContext("task-2", request);

        assertThat(response.removed()).isTrue();
        assertThat(response.removedCount()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(listOperations).rightPushAll(eq(key), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst()).contains("m1");
    }

    @Test
    void deleteContext_Summary_ShouldOnlyDeleteCompressedSystemMessage() {
        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setId("task-3");
        given(taskRecordRepository.findById("task-3")).willReturn(Optional.of(taskRecord));

        String key = "chat:memory:task-3";
        when(listOperations.size(key)).thenReturn(2L);
        when(listOperations.range(key, 0, 1)).thenReturn(List.of(
                "{\"type\":\"SYSTEM\",\"content\":\"[AUTO_COMPRESSED] summary\"}",
                "{\"type\":\"USER\",\"content\":\"keep\"}"));

        ContextDeleteResponse response = contextMutationService.deleteContext("task-3", new ContextDeleteRequest("SUMMARY", null, "remove summary"));

        assertThat(response.removed()).isTrue();
        assertThat(response.removedCount()).isEqualTo(1);
    }
}
