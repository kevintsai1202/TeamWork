package com.teamwork.gateway.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownSubAgentDescriptorRepositoryTest {

    private final MarkdownSubAgentDescriptorRepository repository = new MarkdownSubAgentDescriptorRepository();

    @Test
    void findEnabledDescriptors_ShouldLoadDescriptorsFromMarkdown() {
        List<SubAgentDescriptor> descriptors = repository.findEnabledDescriptors();

        assertThat(descriptors).isNotEmpty();
        assertThat(descriptors).extracting(SubAgentDescriptor::name)
            .contains("general-researcher", "code-implementer", "inline-tools-agent");
        assertThat(descriptors).extracting(SubAgentDescriptor::name)
            .doesNotContain("disabled-agent");
        assertThat(descriptors).allMatch(SubAgentDescriptor::enabled);

        SubAgentDescriptor inlineAgent = descriptors.stream()
            .filter(descriptor -> "inline-tools-agent".equals(descriptor.name()))
            .findFirst()
            .orElseThrow();
        assertThat(inlineAgent.ownerProvider()).isEqualTo("CLAUDE");
        assertThat(inlineAgent.tools()).containsExactly("toolA", "toolB");
        assertThat(inlineAgent.priority()).isEqualTo(5);
    }
}
