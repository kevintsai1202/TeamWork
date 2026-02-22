package com.teamwork.gateway.controller;

import com.teamwork.gateway.dto.AgentContextDetailResponse;
import com.teamwork.gateway.dto.AgentContextUsageItem;
import com.teamwork.gateway.dto.AgentContextUsageResponse;
import com.teamwork.gateway.dto.ContextDeleteRequest;
import com.teamwork.gateway.dto.ContextDeleteResponse;
import com.teamwork.gateway.service.ContextQueryService;
import com.teamwork.gateway.service.ContextMutationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContextControllerTest {

    @Mock
    private ContextQueryService contextQueryService;

    @Mock
    private ContextMutationService contextMutationService;

    @InjectMocks
    private ContextController contextController;

    @Test
    void getContextUsage_ShouldReturnItemsFromService() {
        AgentContextUsageItem usageItem = new AgentContextUsageItem(
                "master-agent",
                "task-1",
                3,
                21,
                0,
                LocalDateTime.of(2026, 2, 23, 12, 30, 0));
        given(contextQueryService.findUsage("master-agent", "task-1", "COMPLETED"))
                .willReturn(List.of(usageItem));

        AgentContextUsageResponse response = contextController.getContextUsage("master-agent", "task-1", "COMPLETED");

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().taskId()).isEqualTo("task-1");
    }

    @Test
    void getFullContext_ShouldReturnDetailFromService() {
        AgentContextDetailResponse detailResponse = new AgentContextDetailResponse(
                "task-1",
                "master-agent",
                "",
                List.of(),
                List.of(),
                List.of());
        given(contextQueryService.getFullContext("task-1")).willReturn(detailResponse);

        AgentContextDetailResponse response = contextController.getFullContext("task-1");

        assertThat(response.taskId()).isEqualTo("task-1");
        assertThat(response.agentName()).isEqualTo("master-agent");
    }

    @Test
    void deleteContext_ShouldReturnDeleteResponseFromService() {
        ContextDeleteRequest request = new ContextDeleteRequest("ALL_HISTORY", null, "cleanup");
        ContextDeleteResponse expected = new ContextDeleteResponse("task-1", true, 3, "audit-1");
        given(contextMutationService.deleteContext("task-1", request)).willReturn(expected);

        ContextDeleteResponse response = contextController.deleteContext("task-1", request);

        assertThat(response.taskId()).isEqualTo("task-1");
        assertThat(response.removed()).isTrue();
        assertThat(response.removedCount()).isEqualTo(3);
        assertThat(response.auditId()).isEqualTo("audit-1");
    }
}
