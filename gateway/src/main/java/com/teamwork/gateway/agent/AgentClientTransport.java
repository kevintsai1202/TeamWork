package com.teamwork.gateway.agent;

/**
 * Agent Client 傳輸抽象。
 *
 * <p>可由不同實作提供（例如 CLI transport、HTTP transport）。</p>
 */
public interface AgentClientTransport {

    /**
     * 執行外部 Agent 呼叫並回傳結果。
     */
    AgentClientResponse execute(AgentClientRequest request);
}
