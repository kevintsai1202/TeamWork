package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;

/**
 * 統一 Agent 介面，讓一般 Agent 與外部 SDK Agent 以一致方式被呼叫。
 */
public interface UnifiedAgentProvider {

    /**
     * 判斷目前 provider 是否支援指定模型設定。
     */
    boolean supports(AiModel aiModel);

    /**
     * 依據統一上下文執行任務，回傳文字結果。
     */
    String execute(AgentExecutionContext context);
}

