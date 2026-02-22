package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Agent 執行上下文，統一承載不同 Agent Provider 所需的輸入資料。
 */
public record AgentExecutionContext(
        String taskId,
        String inputPayload,
        AiModel aiModel,
        ChatModel chatModel,
        String selectedSubAgentName,
        String selectedSubAgentReferencePath,
        String selectedSubAgentOwnerProvider,
        boolean routeFallbackUsed) {
}

