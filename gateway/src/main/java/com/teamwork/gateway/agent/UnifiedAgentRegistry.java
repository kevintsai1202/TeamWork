package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 統一 Agent 註冊表，負責解析本次任務應使用的 Agent Provider。
 */
@Component
public class UnifiedAgentRegistry {

    private final List<UnifiedAgentProvider> providers;

    public UnifiedAgentRegistry(List<UnifiedAgentProvider> providers) {
        this.providers = providers;
    }

    /**
     * 依模型設定選擇可用 provider；找不到時直接拋錯，避免靜默退回錯誤行為。
     */
    public UnifiedAgentProvider resolve(AiModel aiModel) {
        return providers.stream()
                .filter(provider -> provider.supports(aiModel))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No UnifiedAgentProvider supports model provider=" + aiModel.getProvider()));
    }
}

