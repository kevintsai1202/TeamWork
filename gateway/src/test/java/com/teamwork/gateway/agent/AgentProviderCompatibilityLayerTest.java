package com.teamwork.gateway.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentProviderCompatibilityLayerTest {

    private final AgentProviderCompatibilityLayer compatibilityLayer = new AgentProviderCompatibilityLayer();

    @Test
    void normalizeProvider_ShouldMapLegacyAliasToCanonicalValue() {
        assertThat(compatibilityLayer.normalizeProvider("agent-client")).isEqualTo("AGENT_CLIENT");
        assertThat(compatibilityLayer.normalizeProvider("claude-sdk")).isEqualTo("CLAUDE_SDK");
        assertThat(compatibilityLayer.normalizeProvider("claude-agentsdk")).isEqualTo("CLAUDE_AGENT_SDK");
        assertThat(compatibilityLayer.normalizeProvider("spring-ai")).isEqualTo("SPRING_AI");
    }

    @Test
    void normalizeProvider_ShouldKeepUnknownProviderUppercased() {
        assertThat(compatibilityLayer.normalizeProvider("openai")).isEqualTo("OPENAI");
    }

    @Test
    void normalizeProvider_ShouldKeepNullOrBlank() {
        assertThat(compatibilityLayer.normalizeProvider(null)).isNull();
        assertThat(compatibilityLayer.normalizeProvider("   ")).isBlank();
    }
}
