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
    private final UnifiedAgentRegistry unifiedAgentRegistry;
    private final SubAgentDescriptorRepository subAgentDescriptorRepository;
    private final SubAgentRouter subAgentRouter;

    @Async
    @Transactional
    public void processTask(String taskId, String inputPayload) {
        log.info("MasterAgent starts processing Task ID: {}", taskId);

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

                // 3. 先做 sub-agent 路由（AI + 關鍵字雙重比較）。
                var descriptors = subAgentDescriptorRepository.findEnabledDescriptors();
                SubAgentRoutingDecision routingDecision = subAgentRouter.route(inputPayload, chatModel, descriptors);
                log.info("Sub-agent routing decided. name={}, finalScore={}, fallbackUsed={}, reason={}",
                    routingDecision.selected().name(),
                    routingDecision.finalScore(),
                    routingDecision.fallbackUsed(),
                    routingDecision.reason());

                // 4. 透過統一接口選擇 provider 並執行，來源可為一般 Agent 或外部 SDK Agent。
            UnifiedAgentProvider provider = unifiedAgentRegistry.resolve(aiModel);
            log.info("MasterAgent starts AI reasoning with model: {}, provider: {}",
                    aiModel.getName(), aiModel.getProvider());
                String aiResponse = provider.execute(new AgentExecutionContext(
                    taskId,
                    inputPayload,
                    aiModel,
                    chatModel,
                    routingDecision.selected().name(),
                    routingDecision.selected().referencePath(),
                    routingDecision.selected().ownerProvider(),
                    routingDecision.fallbackUsed()));

            log.info("Agent Output: \n{}", aiResponse);

            // 標記為完成 (後續將改為觸發 TaskCompletedEvent，等待 2.3 實作)
            updateTaskStatus(record, "COMPLETED");
            log.info("MasterAgent completed processing Task ID: {}", taskId);

        } catch (Exception e) {
            log.error("MasterAgent encountered an error while processing Task ID: {}", taskId, e);
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
