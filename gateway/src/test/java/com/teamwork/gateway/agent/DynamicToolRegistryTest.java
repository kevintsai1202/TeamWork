package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.ToolConfig;
import com.teamwork.gateway.repository.ToolConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicToolRegistryTest {

    @Mock
    private ToolConfigRepository toolConfigRepository;

    @Mock
    private MasterAgentTools masterAgentTools;

    @Test
    void getActiveToolObjects_WhenNoDbConfig_ShouldEnableAllLocalTools() {
        when(toolConfigRepository.findByType("BUILT_IN")).thenReturn(List.of());
        DynamicToolRegistry registry = new DynamicToolRegistry(toolConfigRepository, masterAgentTools);

        Object[] activeTools = registry.getActiveToolObjects();

        assertThat(activeTools).hasSize(1);
        assertThat(activeTools[0]).isSameAs(masterAgentTools);
    }

    @Test
    void getActiveToolObjects_WhenDbEnabledTrue_ShouldReturnConfiguredTool() {
        ToolConfig config = new ToolConfig();
        config.setName("master-agent-tools");
        config.setType("BUILT_IN");
        config.setConfiguration("{\"enabled\":true}");
        when(toolConfigRepository.findByType("BUILT_IN")).thenReturn(List.of(config));
        DynamicToolRegistry registry = new DynamicToolRegistry(toolConfigRepository, masterAgentTools);

        Object[] activeTools = registry.getActiveToolObjects();

        assertThat(activeTools).hasSize(1);
        assertThat(activeTools[0]).isSameAs(masterAgentTools);
    }

    @Test
    void getEnabledToolNamesFromCache_WhenCalledTwiceWithinTtl_ShouldHitDbOnce() {
        ToolConfig config = new ToolConfig();
        config.setName("master-agent-tools");
        config.setType("BUILT_IN");
        config.setConfiguration("{\"enabled\":true}");
        when(toolConfigRepository.findByType("BUILT_IN")).thenReturn(List.of(config));
        DynamicToolRegistry registry = new DynamicToolRegistry(toolConfigRepository, masterAgentTools);

        List<String> first = registry.getEnabledToolNamesFromCache();
        List<String> second = registry.getEnabledToolNamesFromCache();

        assertThat(first).containsExactly("master-agent-tools");
        assertThat(second).containsExactly("master-agent-tools");
        verify(toolConfigRepository, times(1)).findByType("BUILT_IN");
    }
}

