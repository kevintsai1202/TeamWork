package com.teamwork.gateway.agent;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Provider 相容層：將舊版/別名 provider 值正規化為主流程可識別格式。
 */
@Component
public class AgentProviderCompatibilityLayer {

    private static final Map<String, String> PROVIDER_ALIAS = Map.ofEntries(
            Map.entry("CLAUDE-SDK", "CLAUDE_SDK"),
            Map.entry("CLAUDESDK", "CLAUDE_SDK"),
            Map.entry("CLAUDE_AGENTSDK", "CLAUDE_AGENT_SDK"),
            Map.entry("CLAUDE-AGENT-SDK", "CLAUDE_AGENT_SDK"),
            Map.entry("CLAUDE-AGENTSDK", "CLAUDE_AGENT_SDK"),
            Map.entry("AGENTCLIENT", "AGENT_CLIENT"),
            Map.entry("AGENT-CLIENT", "AGENT_CLIENT"),
            Map.entry("SPRINGAI", "SPRING_AI"),
            Map.entry("SPRING-AI", "SPRING_AI")
    );

    public String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return provider;
        }
        String normalized = provider.trim().toUpperCase(Locale.ROOT);
        return PROVIDER_ALIAS.getOrDefault(normalized, normalized);
    }
}