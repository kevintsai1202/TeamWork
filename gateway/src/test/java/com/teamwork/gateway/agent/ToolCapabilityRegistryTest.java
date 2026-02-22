package com.teamwork.gateway.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ToolCapabilityRegistryTest {

    @Test
    void fromMasterAgentTools_ShouldCreateCatalogWithDefaultTool() {
        MasterAgentTools tools = mock(MasterAgentTools.class);

        ToolCapabilityRegistry registry = ToolCapabilityRegistry.fromMasterAgentTools(tools);

        assertThat(registry.toolCatalog()).containsKey("master-agent-tools");
        assertThat(registry.toolCatalog().get("master-agent-tools")).isSameAs(tools);
        assertThat(registry.allToolObjects()).hasSize(1);
    }

    @Test
    void constructor_WhenDuplicateToolName_ShouldKeepFirstRegistration() {
        Object firstTool = new Object();
        Object secondTool = new Object();

        ToolProviderAdapter adapter1 = () -> List.of(new ToolDescriptor("dup-tool", "BUILT_IN", "A", firstTool));
        ToolProviderAdapter adapter2 = () -> List.of(new ToolDescriptor("dup-tool", "BUILT_IN", "B", secondTool));

        ToolCapabilityRegistry registry = new ToolCapabilityRegistry(List.of(adapter1, adapter2));

        assertThat(registry.toolCatalog()).hasSize(1);
        assertThat(registry.toolCatalog().get("dup-tool")).isSameAs(firstTool);
    }
}
