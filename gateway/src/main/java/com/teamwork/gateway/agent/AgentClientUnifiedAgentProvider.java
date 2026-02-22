package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Agent Client Provider 骨架實作（T15-A1）。
 *
 * <p>本類別先建立統一介面接點，實際傳輸由 {@link AgentClientTransport} 注入實作。</p>
 */
@Component
@Order(20)
public class AgentClientUnifiedAgentProvider implements UnifiedAgentProvider {

    private final ObjectProvider<AgentClientTransport> transportProvider;

    public AgentClientUnifiedAgentProvider(ObjectProvider<AgentClientTransport> transportProvider) {
        this.transportProvider = transportProvider;
    }

    /**
     * 僅在模型 provider 指定為 AGENT_CLIENT 時命中。
     */
    @Override
    public boolean supports(AiModel aiModel) {
        if (aiModel == null || aiModel.getProvider() == null) {
            return false;
        }
        String normalized = aiModel.getProvider().trim().toUpperCase();
        return "AGENT_CLIENT".equals(normalized);
    }

    /**
     * 透過 transport 抽象呼叫外部 agent；若尚未注入 transport，回傳明確錯誤。
     */
    @Override
    public String execute(AgentExecutionContext context) {
        AgentClientTransport transport = transportProvider.getIfAvailable();
        if (transport == null) {
            throw new UnsupportedOperationException(
                    "AgentClient transport is not configured. Please provide an AgentClientTransport implementation.");
        }

        AgentClientRequest request = new AgentClientRequest(
                context.taskId(),
                context.aiModel() != null ? context.aiModel().getName() : null,
                context.aiModel() != null ? context.aiModel().getProvider() : null,
                context.inputPayload(),
                context.selectedSubAgentName(),
                context.selectedSubAgentReferencePath(),
                context.sandboxEnabled(),
                context.resolvedSandboxType());

        AgentClientResponse response = transport.execute(request);
        return response != null ? response.content() : null;
    }
}
