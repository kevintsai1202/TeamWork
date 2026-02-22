package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 統一 Agent 註冊表，負責解析本次任務應使用的 Agent Provider。
 * <p>T14 擴充：新增 resolve(AgentExecutionContext) 方法，
 * 當 ctx.sandboxEnabled() = true 時優先路由至 SandboxExecutionProvider。</p>
 */
@Component
public class UnifiedAgentRegistry {

    private final List<UnifiedAgentProvider> providers;
    /** 沙盒 Provider 獨立注入，用於 AgentProfile 觸發的路由判斷 */
    private final SandboxExecutionProvider sandboxExecutionProvider;
    private final AgentProviderCompatibilityLayer compatibilityLayer;

    public UnifiedAgentRegistry(List<UnifiedAgentProvider> providers,
                                SandboxExecutionProvider sandboxExecutionProvider,
                                AgentProviderCompatibilityLayer compatibilityLayer) {
        this.providers = providers;
        this.sandboxExecutionProvider = sandboxExecutionProvider;
        this.compatibilityLayer = compatibilityLayer;
    }

    /**
     * 依模型設定選擇可用 provider（舊版相容介面）；
     * 找不到時直接拋錯，避免靜默退回錯誤行為。
     */
    public UnifiedAgentProvider resolve(AiModel aiModel) {
        AiModel normalizedModel = normalizeModelProvider(aiModel);
        return providers.stream()
            .filter(provider -> provider.supports(normalizedModel))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                "No UnifiedAgentProvider supports model provider=" + normalizedModel.getProvider()));
    }

    /**
     * 依執行上下文選擇 provider（T14 新版）：
     * <ol>
     *   <li>若 ctx.sandboxEnabled() = true，優先路由至 SandboxExecutionProvider</li>
     *   <li>否則回退至原本的 AiModel provider 路由</li>
     * </ol>
     *
     * @param ctx 包含 sandboxEnabled 及 aiModel 的執行上下文
     * @return 選定的 UnifiedAgentProvider
     * @throws IllegalStateException 找不到任何可用 provider
     */
    public UnifiedAgentProvider resolve(AgentExecutionContext ctx) {
        if (ctx.sandboxEnabled()) {
            return sandboxExecutionProvider;
        }
        return resolve(ctx.aiModel());
    }

    private AiModel normalizeModelProvider(AiModel original) {
        if (original == null) {
            return null;
        }
        AiModel normalized = new AiModel();
        normalized.setId(original.getId());
        normalized.setName(original.getName());
        normalized.setApiKey(original.getApiKey());
        normalized.setEndpointUrl(original.getEndpointUrl());
        normalized.setActive(original.isActive());
        normalized.setProvider(compatibilityLayer.normalizeProvider(original.getProvider()));
        return normalized;
    }
}


