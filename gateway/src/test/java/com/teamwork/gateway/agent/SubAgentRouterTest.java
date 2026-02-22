package com.teamwork.gateway.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SubAgentRouterTest {

    private final HybridRoutingProperties properties = new HybridRoutingProperties();

    @Test
    void route_ShouldPickHighestHybridScore() {
        properties.setAiWeight(0.6);
        properties.setKeywordWeight(0.4);
                properties.setThreshold(0.50);

        SubAgentSemanticScorer scorer = (input, descriptors, chatModel) -> Map.of(
                "general-researcher", 0.2,
                "code-implementer", 0.9);
        SubAgentRouter router = new SubAgentRouter(properties, scorer);

        SubAgentDescriptor researcher = new SubAgentDescriptor(
                "general-researcher", "研究分析", List.of("search"), "CLAUDE", "agents/subagents/general-researcher.md", true, 0);
        SubAgentDescriptor coder = new SubAgentDescriptor(
                "code-implementer", "程式實作修正", List.of("code"), "CLAUDE", "agents/subagents/code-implementer.md", true, 10);

        SubAgentRoutingDecision decision = router.route("code implement and test", null, List.of(researcher, coder));

        assertThat(decision.selected().name()).isEqualTo("code-implementer");
        assertThat(decision.fallbackUsed()).isFalse();
    }

    @Test
    void route_ShouldFallbackWhenScoreBelowThreshold() {
        properties.setAiWeight(0.6);
        properties.setKeywordWeight(0.4);
        properties.setThreshold(0.95);

        SubAgentSemanticScorer scorer = (input, descriptors, chatModel) -> Map.of(
                "general-researcher", 0.4,
                "code-implementer", 0.4);
        SubAgentRouter router = new SubAgentRouter(properties, scorer);

        SubAgentDescriptor researcher = new SubAgentDescriptor(
                "general-researcher", "研究分析", List.of("search"), "CLAUDE", "agents/subagents/general-researcher.md", true, 0);
        SubAgentDescriptor coder = new SubAgentDescriptor(
                "code-implementer", "程式實作修正", List.of("code"), "CLAUDE", "agents/subagents/code-implementer.md", true, 10);

        SubAgentRoutingDecision decision = router.route("隨機問題", null, List.of(researcher, coder));

        assertThat(decision.selected().name()).isEqualTo("general-researcher");
        assertThat(decision.fallbackUsed()).isTrue();
        assertThat(decision.reason()).isEqualTo("below_threshold");
    }

    @Test
    void route_ShouldResolveTieByPriority() {
        properties.setAiWeight(0.6);
        properties.setKeywordWeight(0.4);
        properties.setThreshold(0.20);

        SubAgentSemanticScorer scorer = (input, descriptors, chatModel) -> Map.of(
                "general-researcher", 0.5,
                "code-implementer", 0.5);
        SubAgentRouter router = new SubAgentRouter(properties, scorer);

        SubAgentDescriptor researcher = new SubAgentDescriptor(
                "general-researcher", "一般任務", List.of("task"), "CLAUDE", "agents/subagents/general-researcher.md", true, 0);
        SubAgentDescriptor coder = new SubAgentDescriptor(
                "code-implementer", "一般任務", List.of("task"), "CLAUDE", "agents/subagents/code-implementer.md", true, 10);

        SubAgentRoutingDecision decision = router.route("task", null, List.of(researcher, coder));

        assertThat(decision.selected().name()).isEqualTo("code-implementer");
    }

        @Test
        void route_ShouldUseDefaultWhenNoDescriptorProvided() {
                SubAgentSemanticScorer scorer = (input, descriptors, chatModel) -> Map.of();
                SubAgentRouter router = new SubAgentRouter(properties, scorer);

                SubAgentRoutingDecision decision = router.route("anything", null, List.of());

                assertThat(decision.selected().name()).isEqualTo("general-researcher");
                assertThat(decision.fallbackUsed()).isTrue();
                assertThat(decision.reason()).isEqualTo("no_descriptor");
        }

        @Test
        void route_ShouldUseFallbackWhenWeightsInvalid() {
                properties.setAiWeight(0);
                properties.setKeywordWeight(0);
                properties.setThreshold(0.5);

                SubAgentSemanticScorer scorer = (input, descriptors, chatModel) -> Map.of(
                                "custom-agent", 0.9);
                SubAgentRouter router = new SubAgentRouter(properties, scorer);

                SubAgentDescriptor custom = new SubAgentDescriptor(
                                "custom-agent", "custom", List.of("x"), "CLAUDE", "agents/subagents/custom.md", true, 3);

                SubAgentRoutingDecision decision = router.route("none", null, List.of(custom));

                assertThat(decision.selected().name()).isEqualTo("custom-agent");
                assertThat(decision.fallbackUsed()).isFalse();
        }

        @Test
        void route_ShouldFallbackToFirstWhenGeneralNotExists() {
                properties.setAiWeight(0.6);
                properties.setKeywordWeight(0.4);
                properties.setThreshold(0.9);

                SubAgentSemanticScorer scorer = (input, descriptors, chatModel) -> Map.of("alpha", 0.1);
                SubAgentRouter router = new SubAgentRouter(properties, scorer);

                SubAgentDescriptor alpha = new SubAgentDescriptor(
                                "alpha", "alpha", List.of(), "CLAUDE", "agents/subagents/alpha.md", true, 1);

                SubAgentRoutingDecision decision = router.route("zzz", null, List.of(alpha));

                assertThat(decision.selected().name()).isEqualTo("general-researcher");
                assertThat(decision.fallbackUsed()).isTrue();
        }
}
