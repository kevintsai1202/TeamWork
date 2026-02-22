package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentClientUnifiedAgentProviderTest {

    @Test
    void supports_ShouldReturnTrue_WhenProviderIsAgentClient() {
        ObjectProvider<AgentClientTransport> objectProvider = mock(ObjectProvider.class);
        AgentClientUnifiedAgentProvider provider = new AgentClientUnifiedAgentProvider(objectProvider);

        AiModel model = new AiModel();
        model.setProvider("AGENT_CLIENT");

        assertThat(provider.supports(model)).isTrue();
    }

    @Test
    void supports_ShouldReturnFalse_WhenProviderIsNotAgentClient() {
        ObjectProvider<AgentClientTransport> objectProvider = mock(ObjectProvider.class);
        AgentClientUnifiedAgentProvider provider = new AgentClientUnifiedAgentProvider(objectProvider);

        AiModel model = new AiModel();
        model.setProvider("OPENAI");

        assertThat(provider.supports(model)).isFalse();
    }

    @Test
    void execute_ShouldThrow_WhenTransportNotConfigured() {
        ObjectProvider<AgentClientTransport> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(null);

        AgentClientUnifiedAgentProvider provider = new AgentClientUnifiedAgentProvider(objectProvider);

        AiModel model = new AiModel();
        model.setName("m");
        model.setProvider("AGENT_CLIENT");

        AgentExecutionContext ctx = new AgentExecutionContext(
                "task-1",
                "payload",
                model,
                null,
                "sub",
                "agents/sub.md",
                "CLAUDE",
                false,
                false,
                "",
                "payload",
                0L,
                "LOCAL",
                null);

        assertThatThrownBy(() -> provider.execute(ctx))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("AgentClient transport is not configured");
    }
}
