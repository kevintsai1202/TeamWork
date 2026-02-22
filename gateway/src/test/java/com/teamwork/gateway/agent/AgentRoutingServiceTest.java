package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AgentProfile;
import com.teamwork.gateway.entity.AiModel;
import com.teamwork.gateway.repository.AgentProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentRoutingServiceTest {

    @Mock
    private SubAgentDescriptorRepository subAgentDescriptorRepository;

    @Mock
    private SubAgentRouter subAgentRouter;

    @Mock
    private AgentProfileRepository agentProfileRepository;

    @Mock
    private UnifiedAgentRegistry unifiedAgentRegistry;

    @Mock
    private ChatModel chatModel;

    @Mock
    private UnifiedAgentProvider unifiedAgentProvider;

    @InjectMocks
    private AgentRoutingService agentRoutingService;

    @Test
    void plan_WhenProfileNotFound_ShouldFallbackToNonSandboxAndLocalType() {
        AiModel aiModel = new AiModel();
        aiModel.setProvider("OPENAI");
        aiModel.setName("gpt");

        SubAgentDescriptor descriptor = new SubAgentDescriptor(
                "general-researcher",
                "desc",
                List.of("tool-a"),
                "CLAUDE",
                "agents/subagents/general-researcher.md",
                true,
                1);

        SubAgentRoutingDecision decision = new SubAgentRoutingDecision(
                descriptor,
                0.5,
                0.6,
                0.56,
                false,
                "matched");

        when(subAgentDescriptorRepository.findEnabledDescriptors()).thenReturn(List.of(descriptor));
        when(subAgentRouter.route("hello", chatModel, List.of(descriptor))).thenReturn(decision);
        when(agentProfileRepository.findById("profile-1")).thenReturn(Optional.empty());
        when(unifiedAgentRegistry.resolve(org.mockito.ArgumentMatchers.any(AgentExecutionContext.class)))
                .thenReturn(unifiedAgentProvider);

        RoutingPlan plan = agentRoutingService.plan("task-1", "hello", "profile-1", aiModel, chatModel);

        assertThat(plan).isNotNull();
        assertThat(plan.provider()).isSameAs(unifiedAgentProvider);
        assertThat(plan.subAgentRoutingDecision().selected().name()).isEqualTo("general-researcher");
        assertThat(plan.executionContext().sandboxEnabled()).isFalse();
        assertThat(plan.executionContext().resolvedSandboxType()).isEqualTo("LOCAL");
        assertThat(plan.executionContext().selectedSubAgentReferencePath())
                .isEqualTo("agents/subagents/general-researcher.md");
    }

    @Test
    void plan_WhenSandboxProfileEnabled_ShouldBuildSandboxContextWithExtractedLanguage() {
        AiModel aiModel = new AiModel();
        aiModel.setProvider("OPENAI");
        aiModel.setName("gpt");

        AgentProfile profile = new AgentProfile();
        profile.setId("profile-2");
        profile.setSandboxEnabled(true);
        profile.setSandboxType("DOCKER");
        profile.setDockerImage("ghcr.io/demo/runtime:latest");

        SubAgentDescriptor descriptor = new SubAgentDescriptor(
                "code-implementer",
                "desc",
                List.of("tool-code"),
                "CLAUDE",
                "agents/subagents/code-implementer.md",
                true,
                1);

        SubAgentRoutingDecision decision = new SubAgentRoutingDecision(
                descriptor,
                0.7,
                0.8,
                0.75,
                false,
                "matched");

        String payload = "[lang:python] print('ok')";

        when(subAgentDescriptorRepository.findEnabledDescriptors()).thenReturn(List.of(descriptor));
        when(subAgentRouter.route(payload, chatModel, List.of(descriptor))).thenReturn(decision);
        when(agentProfileRepository.findById("profile-2")).thenReturn(Optional.of(profile));
        when(unifiedAgentRegistry.resolve(org.mockito.ArgumentMatchers.any(AgentExecutionContext.class)))
                .thenReturn(unifiedAgentProvider);

        RoutingPlan plan = agentRoutingService.plan("task-2", payload, "profile-2", aiModel, chatModel);

        assertThat(plan.executionContext().sandboxEnabled()).isTrue();
        assertThat(plan.executionContext().resolvedSandboxType()).isEqualTo("DOCKER");
        assertThat(plan.executionContext().sandboxDockerImage()).isEqualTo("ghcr.io/demo/runtime:latest");
        assertThat(plan.executionContext().sandboxLanguage()).isEqualTo("python");
        assertThat(plan.executionContext().selectedSubAgentName()).isEqualTo("code-implementer");
        assertThat(plan.provider()).isSameAs(unifiedAgentProvider);
    }
}
