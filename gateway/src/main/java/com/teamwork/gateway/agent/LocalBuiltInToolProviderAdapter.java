package com.teamwork.gateway.agent;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地內建工具 provider。
 */
@Component
public class LocalBuiltInToolProviderAdapter implements ToolProviderAdapter {

    private static final String BUILT_IN_TOOL_TYPE = "BUILT_IN";
    private static final String LOCAL_PROVIDER = "LOCAL";

    private final MasterAgentTools masterAgentTools;

    public LocalBuiltInToolProviderAdapter(MasterAgentTools masterAgentTools) {
        this.masterAgentTools = masterAgentTools;
    }

    @Override
    public List<ToolDescriptor> listTools() {
        return List.of(new ToolDescriptor(
                "master-agent-tools",
                BUILT_IN_TOOL_TYPE,
                LOCAL_PROVIDER,
                masterAgentTools));
    }
}
