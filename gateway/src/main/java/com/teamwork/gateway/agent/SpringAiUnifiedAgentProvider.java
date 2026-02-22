package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import com.teamwork.gateway.memory.RedisChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springaicommunity.agent.tools.task.TaskToolCallbackProvider;
import org.springaicommunity.agent.tools.task.subagent.SubagentReference;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * 內建 Spring AI Provider，封裝目前既有 ChatClient + Tool Calling 執行流程。
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class SpringAiUnifiedAgentProvider implements UnifiedAgentProvider {

    private final RedisChatMemory redisChatMemory;
    private final DynamicToolRegistry dynamicToolRegistry;

    /**
     * 除了明確標記為 Claude SDK 的 provider，其餘皆由 Spring AI Provider 處理。
     */
    @Override
    public boolean supports(AiModel aiModel) {
        String provider = aiModel.getProvider();
        if (provider == null) {
            return true;
        }
        String normalized = provider.trim().toUpperCase();
        return !"CLAUDE_SDK".equals(normalized) && !"CLAUDE_AGENT_SDK".equals(normalized);
    }

    /**
     * 執行標準 Spring AI 對話流程，並掛上記憶體、動態工具與 sub-agent callbacks。
     */
    @Override
    public String execute(AgentExecutionContext context) {
        ChatClient chatClient = ChatClient.create(context.chatModel()).mutate()
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(redisChatMemory)
                        .conversationId(context.taskId())
                        .order(10)
                        .scheduler(Schedulers.boundedElastic())
                        .build())
                .build();

        Object[] activeTools = dynamicToolRegistry.getActiveToolObjects();
    ToolCallbackProvider taskToolProvider = buildTaskToolProvider(
        context.chatModel(),
        context.selectedSubAgentReferencePath(),
        context.selectedSubAgentOwnerProvider());
        ChatClientRequestSpec requestSpec = chatClient.prompt();
        if (activeTools.length > 0) {
            requestSpec = requestSpec.tools(activeTools);
        }
        return requestSpec
                .toolCallbacks(taskToolProvider)
                .user(context.inputPayload())
                .call()
                .content();
    }

    /**
     * 建立 Task Tool Provider，讓主代理可委派至 markdown 定義的 sub-agent。
     */
        private ToolCallbackProvider buildTaskToolProvider(
            org.springframework.ai.chat.model.ChatModel chatModel,
            String selectedReferencePath,
            String selectedOwnerProvider) {
        String referencePath =
            (selectedReferencePath == null || selectedReferencePath.isBlank())
                ? "agents/subagents/general-researcher.md"
                : selectedReferencePath;
        String ownerProvider =
            (selectedOwnerProvider == null || selectedOwnerProvider.isBlank())
                ? "CLAUDE"
                : selectedOwnerProvider;

        List<SubagentReference> subagentReferences = List.of(
            new SubagentReference(referencePath, ownerProvider));

        return TaskToolCallbackProvider.builder()
                .chatClientBuilders(Map.of("default", ChatClient.builder(chatModel)))
                .subagentReferences(subagentReferences)
                .build();
    }
}

