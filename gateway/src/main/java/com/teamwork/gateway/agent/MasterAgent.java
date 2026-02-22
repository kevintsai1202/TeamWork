package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.event.TaskStatusChangeEvent;
import com.teamwork.gateway.repository.TaskRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import com.teamwork.gateway.ai.ChatModelFactory;
import com.teamwork.gateway.entity.AiModel;
import com.teamwork.gateway.repository.AiModelRepository;
import org.springframework.ai.chat.model.ChatModel;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterAgent {

    private final TaskRecordRepository taskRecordRepository;
    private final ApplicationEventPublisher eventPublisher;
    // -- Phase 2 Components --
    private final AiModelRepository aiModelRepository;
    private final ChatModelFactory chatModelFactory;
    private final AgentRoutingService agentRoutingService;
    private final AgentObservabilityService agentObservabilityService;

    @Async
    @Transactional
    public void processTask(String taskId, String inputPayload) {
        log.info("MasterAgent starts processing Task ID: {}", taskId);
        long startedAt = System.currentTimeMillis();
        agentObservabilityService.recordTaskStarted(taskId);

        Optional<TaskRecord> recordOpt = taskRecordRepository.findById(taskId);
        if (recordOpt.isEmpty()) {
            log.error("TaskRecord not found for ID: {}. Aborting process.", taskId);
            return;
        }

        TaskRecord record = recordOpt.get();
        try {
            updateTaskStatus(record, "RUNNING");

            // 1. 從 DB 取得啟用的 AI Model (這裡先簡單取第一筆 Active 測試)
            AiModel aiModel = aiModelRepository.findAll().stream()
                    .filter(AiModel::isActive)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active AI model found in DB"));

            // 2. 透過 Factory 初始化真正的 ChatModel
            ChatModel chatModel = chatModelFactory.createChatModel(aiModel);

                // 3. 路由決策（T18-4）：由 AgentRoutingService 集中處理
                RoutingPlan routingPlan = agentRoutingService.plan(
                    taskId,
                    inputPayload,
                    record.getProfileId(),
                    aiModel,
                    chatModel);

                AgentExecutionContext ctx = routingPlan.executionContext();
                UnifiedAgentProvider provider = routingPlan.provider();

                log.info("Sub-agent routing decided. name={}, finalScore={}, fallbackUsed={}, reason={}",
                    routingPlan.subAgentRoutingDecision().selected().name(),
                    routingPlan.subAgentRoutingDecision().finalScore(),
                    routingPlan.subAgentRoutingDecision().fallbackUsed(),
                    routingPlan.subAgentRoutingDecision().reason());

            log.info("MasterAgent starts AI reasoning with model: {}, provider: {}, sandboxEnabled: {}",
                    aiModel.getName(), aiModel.getProvider(), ctx.sandboxEnabled());

            String aiResponse = provider.execute(ctx);

            log.info("Agent Output: \n{}", aiResponse);

            // 標記為完成
            updateTaskStatus(record, "COMPLETED");
            agentObservabilityService.recordTaskCompleted(taskId, System.currentTimeMillis() - startedAt);
            log.info("MasterAgent completed processing Task ID: {}", taskId);

        } catch (Exception e) {
            log.error("MasterAgent encountered an error while processing Task ID: {}", taskId, e);
            agentObservabilityService.recordTaskFailed(taskId, e);
            updateTaskStatus(record, "FAILED");
        }
    }

    private void updateTaskStatus(TaskRecord record, String newStatus) {
        record.setStatus(newStatus);
        taskRecordRepository.save(record);
        log.info("Task status updated. TaskId: {}, Status: {}", record.getId(), newStatus);

        // 放出狀態更新事件給 SSE (或其他 Listener) 訂閱
        eventPublisher.publishEvent(new TaskStatusChangeEvent(this, record.getId(), newStatus, null));
    }
}

