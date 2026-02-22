package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnifiedAgentRegistryTest {

    private AgentProviderCompatibilityLayer compatibilityLayer() {
        return new AgentProviderCompatibilityLayer();
    }

    /** 建立 registry 用的 sandbox provider stub（supports=false，不影響 AiModel 路由測試）*/
    private SandboxExecutionProvider sandboxStub() {
        return mock(SandboxExecutionProvider.class);
    }

    @Test
    void resolve_ShouldReturnFirstSupportingProvider() {
        UnifiedAgentProvider first = mock(UnifiedAgentProvider.class);
        UnifiedAgentProvider second = mock(UnifiedAgentProvider.class);
        // T14：UnifiedAgentRegistry 需要兩個參數（providers + sandboxProvider）
        UnifiedAgentRegistry registry = new UnifiedAgentRegistry(List.of(first, second), sandboxStub(), compatibilityLayer());

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
        // T14：UnifiedAgentRegistry 需要兩個參數
        UnifiedAgentRegistry registry = new UnifiedAgentRegistry(List.of(first), sandboxStub(), compatibilityLayer());

        AiModel aiModel = new AiModel();
        aiModel.setProvider("UNKNOWN");

        when(first.supports(aiModel)).thenReturn(false);

        assertThatThrownBy(() -> registry.resolve(aiModel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No UnifiedAgentProvider supports model provider=UNKNOWN");
    }

    @Test
    void resolve_WithContext_WhenSandboxEnabled_ShouldReturnSandboxProvider() {
        // Arrange
        SandboxExecutionProvider sandboxProvider = mock(SandboxExecutionProvider.class);
        UnifiedAgentProvider aiProvider = mock(UnifiedAgentProvider.class);
        UnifiedAgentRegistry registry = new UnifiedAgentRegistry(List.of(aiProvider), sandboxProvider, compatibilityLayer());

        // 建構 sandboxEnabled=true 的上下文
        AgentExecutionContext ctx = new AgentExecutionContext(
                "task-sb", "payload", null, null,
                "agent", "", "spring-ai", false,
                true, "python", "print(1)", 0L,
                "LOCAL", null);

        // Act
        UnifiedAgentProvider resolved = registry.resolve(ctx);

        // Assert
        assertThat(resolved).isSameAs(sandboxProvider);
    }

    @Test
    void resolve_WithContext_WhenSandboxDisabled_ShouldUseAiModelRouting() {
        // Arrange
        SandboxExecutionProvider sandboxProvider = mock(SandboxExecutionProvider.class);
        UnifiedAgentProvider aiProvider = mock(UnifiedAgentProvider.class);
        UnifiedAgentRegistry registry = new UnifiedAgentRegistry(List.of(aiProvider), sandboxProvider, compatibilityLayer());

        AiModel aiModel = new AiModel();
        aiModel.setProvider("OPENAI");
        when(aiProvider.supports(aiModel)).thenReturn(true);

        // 建構 sandboxEnabled=false 的上下文
        AgentExecutionContext ctx = new AgentExecutionContext(
                "task-ai", "payload", aiModel, null,
                "agent", "", "spring-ai", false,
                false, null, null, 0L,
                null, null);

        // Act
        UnifiedAgentProvider resolved = registry.resolve(ctx);

        // Assert
        assertThat(resolved).isSameAs(aiProvider);
    }

    @Test
    void resolve_ShouldNormalizeLegacyProviderAlias() {
        UnifiedAgentProvider aiProvider = mock(UnifiedAgentProvider.class);
        when(aiProvider.supports(org.mockito.ArgumentMatchers.any(AiModel.class))).thenAnswer(invocation -> {
            AiModel model = invocation.getArgument(0);
            return "AGENT_CLIENT".equals(model.getProvider());
        });

        UnifiedAgentRegistry registry = new UnifiedAgentRegistry(List.of(aiProvider), sandboxStub(), compatibilityLayer());

        AiModel aiModel = new AiModel();
        aiModel.setProvider("agent-client");

        UnifiedAgentProvider resolved = registry.resolve(aiModel);
        assertThat(resolved).isSameAs(aiProvider);
    }
}

