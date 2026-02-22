package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Claude Agent SDK Provider 佔位實作。
 * 後續接入 claude-agent-sdk-java 時，可在此類別內完成真正的 SDK 呼叫。
 */
@Component
@Order(10)
public class ClaudeSdkUnifiedAgentProvider implements UnifiedAgentProvider {

    /**
     * 僅在模型 provider 顯式指定為 Claude SDK 時命中。
     */
    @Override
    public boolean supports(AiModel aiModel) {
        String provider = aiModel.getProvider();
        if (provider == null) {
            return false;
        }
        String normalized = provider.trim().toUpperCase();
        return "CLAUDE_SDK".equals(normalized) || "CLAUDE_AGENT_SDK".equals(normalized);
    }

    /**
     * 目前先保留統一入口，未接入 SDK 前回傳明確錯誤訊息。
     */
    @Override
    public String execute(AgentExecutionContext context) {
        throw new UnsupportedOperationException(
                "Claude Agent SDK provider is not implemented yet. Please integrate claude-agent-sdk-java first.");
    }
}

