package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaudeSdkUnifiedAgentProviderTest {

    private final ClaudeSdkUnifiedAgentProvider provider = new ClaudeSdkUnifiedAgentProvider();

    @Test
    void supports_ShouldReturnFalseWhenProviderIsNull() {
        AiModel aiModel = new AiModel();
        aiModel.setProvider(null);

        assertThat(provider.supports(aiModel)).isFalse();
    }

    @Test
    void supports_ShouldReturnTrueForClaudeProviders() {
        AiModel aiModel1 = new AiModel();
        aiModel1.setProvider("CLAUDE_SDK");
        assertThat(provider.supports(aiModel1)).isTrue();

        AiModel aiModel2 = new AiModel();
        aiModel2.setProvider("claude_agent_sdk");
        assertThat(provider.supports(aiModel2)).isTrue();
    }

    @Test
    void supports_ShouldReturnFalseForOtherProviders() {
        AiModel aiModel = new AiModel();
        aiModel.setProvider("OPENAI");

        assertThat(provider.supports(aiModel)).isFalse();
    }

    @Test
    void execute_ShouldThrowUnsupportedOperationException() {
        assertThatThrownBy(() -> provider.execute(null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not implemented yet");
    }
}
