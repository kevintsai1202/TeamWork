package com.teamwork.gateway.controller;

import com.teamwork.gateway.dto.AgentContextDetailResponse;
import com.teamwork.gateway.dto.AgentContextUsageResponse;
import com.teamwork.gateway.dto.ContextDeleteRequest;
import com.teamwork.gateway.dto.ContextDeleteResponse;
import com.teamwork.gateway.service.ContextMutationService;
import com.teamwork.gateway.service.ContextQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agents/context")
@RequiredArgsConstructor
public class ContextController {

    private final ContextQueryService contextQueryService;
    private final ContextMutationService contextMutationService;

    @GetMapping("/usage")
    public AgentContextUsageResponse getContextUsage(
            @RequestParam(required = false) String agentName,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String status) {
        return new AgentContextUsageResponse(contextQueryService.findUsage(agentName, taskId, status));
    }

    @GetMapping("/{taskId}")
    public AgentContextDetailResponse getFullContext(@PathVariable String taskId) {
        return contextQueryService.getFullContext(taskId);
    }

    @DeleteMapping("/{taskId}")
    public ContextDeleteResponse deleteContext(@PathVariable String taskId, @RequestBody ContextDeleteRequest request) {
        return contextMutationService.deleteContext(taskId, request);
    }
}
