package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.ToolConfig;
import com.teamwork.gateway.repository.ToolConfigRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AskUserToolPolicyServiceTest {

    @Test
    void canAskQuestion_WhenConfigEnabledTrue_ShouldReturnTrue() {
        ToolConfigRepository repository = mock(ToolConfigRepository.class);
        ToolConfig config = new ToolConfig();
        config.setName(AskUserToolPolicyService.TOOL_ASK_QUESTION);
        config.setType("BUILT_IN");
        config.setConfiguration("{\"enabled\":true}");
        when(repository.findByType("BUILT_IN")).thenReturn(List.of(config));

        AskUserToolPolicyService service = new AskUserToolPolicyService(repository);

        assertThat(service.canAskQuestion()).isTrue();
    }

    @Test
    void canSubmitAnswer_WhenNoConfig_ShouldReturnFalse() {
        ToolConfigRepository repository = mock(ToolConfigRepository.class);
        when(repository.findByType("BUILT_IN")).thenReturn(List.of());

        AskUserToolPolicyService service = new AskUserToolPolicyService(repository);

        assertThat(service.canSubmitAnswer()).isFalse();
    }

    @Test
    void policyCache_ShouldHitDbOnceWithinTtl() {
        ToolConfigRepository repository = mock(ToolConfigRepository.class);
        ToolConfig config = new ToolConfig();
        config.setName(AskUserToolPolicyService.TOOL_LIST_PENDING);
        config.setType("BUILT_IN");
        config.setConfiguration("{\"enabled\":true}");
        when(repository.findByType("BUILT_IN")).thenReturn(List.of(config));

        AskUserToolPolicyService service = new AskUserToolPolicyService(repository);

        assertThat(service.canListPending()).isTrue();
        assertThat(service.canListPending()).isTrue();

        verify(repository, times(1)).findByType("BUILT_IN");
    }

    @Test
    void canGetCurrentTime_WhenNoConfig_ShouldDefaultTrue() {
        ToolConfigRepository repository = mock(ToolConfigRepository.class);
        when(repository.findByType("BUILT_IN")).thenReturn(List.of());

        AskUserToolPolicyService service = new AskUserToolPolicyService(repository);

        assertThat(service.canGetCurrentTime()).isTrue();
    }

    @Test
    void canReadSkillContent_WhenConfigEnabledFalse_ShouldReturnFalse() {
        ToolConfigRepository repository = mock(ToolConfigRepository.class);
        ToolConfig config = new ToolConfig();
        config.setName(AskUserToolPolicyService.TOOL_READ_SKILL_CONTENT);
        config.setType("BUILT_IN");
        config.setConfiguration("{\"enabled\":false}");
        when(repository.findByType("BUILT_IN")).thenReturn(List.of(config));

        AskUserToolPolicyService service = new AskUserToolPolicyService(repository);

        assertThat(service.canReadSkillContent()).isFalse();
    }
}
