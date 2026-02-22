package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnifiedAgentRegistryTest {

    @Test
    void resolve_ShouldReturnFirstSupportingProvider() {
        UnifiedAgentProvider first = mock(UnifiedAgentProvider.class);
        UnifiedAgentProvider second = mock(UnifiedAgentProvider.class);
        UnifiedAgentRegistry registry = new UnifiedAgentRegistry(List.of(first, second));

        AiModel aiModel = new AiModel();
        aiModel.setProvider("OPENAI");

        when(first.supports(aiModel)).thenReturn(false);
        when(second.supports(aiModel)).thenReturn(true);

        UnifiedAgentProvider resolved = registry.resolve(aiModel);

        assertThat(resolved).isSameAs(second);
    }

    @Test
    void resolve_ShouldThrowWhenNoProviderSupportsModel() {
        UnifiedAgentProvider first = mock(UnifiedAgentProvider.class);
        UnifiedAgentRegistry registry = new UnifiedAgentRegistry(List.of(first));

        AiModel aiModel = new AiModel();
        aiModel.setProvider("UNKNOWN");

        when(first.supports(aiModel)).thenReturn(false);

        assertThatThrownBy(() -> registry.resolve(aiModel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No UnifiedAgentProvider supports model provider=UNKNOWN");
    }
}
