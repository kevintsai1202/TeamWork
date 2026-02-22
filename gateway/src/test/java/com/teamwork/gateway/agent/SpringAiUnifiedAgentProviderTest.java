package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SpringAiUnifiedAgentProviderTest {

    @Test
    void supports_ShouldReturnTrueWhenProviderIsNull() {
        SpringAiUnifiedAgentProvider provider = new SpringAiUnifiedAgentProvider(mock(), mock());
        AiModel aiModel = new AiModel();
        aiModel.setProvider(null);

        assertThat(provider.supports(aiModel)).isTrue();
    }

    @Test
    void supports_ShouldReturnFalseForClaudeProviders() {
        SpringAiUnifiedAgentProvider provider = new SpringAiUnifiedAgentProvider(mock(), mock());

        AiModel aiModel1 = new AiModel();
        aiModel1.setProvider("CLAUDE_SDK");
        assertThat(provider.supports(aiModel1)).isFalse();

        AiModel aiModel2 = new AiModel();
        aiModel2.setProvider("claude_agent_sdk");
        assertThat(provider.supports(aiModel2)).isFalse();
    }

    @Test
    void supports_ShouldReturnTrueForNonClaudeProvider() {
        SpringAiUnifiedAgentProvider provider = new SpringAiUnifiedAgentProvider(mock(), mock());
        AiModel aiModel = new AiModel();
        aiModel.setProvider("OPENAI");

        assertThat(provider.supports(aiModel)).isTrue();
    }
}
