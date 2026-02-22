package com.teamwork.gateway.agent;

import java.util.List;

/**
 * Sub-agent 描述資訊。
 */
public record SubAgentDescriptor(
        String name,
        String description,
        List<String> tools,
        String ownerProvider,
        String referencePath,
        boolean enabled,
        int priority) {
}
