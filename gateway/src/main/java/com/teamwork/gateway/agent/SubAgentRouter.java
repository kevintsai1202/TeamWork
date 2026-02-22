package com.teamwork.gateway.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sub-agent Hybrid 路由器（AI 語意 + 關鍵字規則）。
 */
@Component
@RequiredArgsConstructor
public class SubAgentRouter {

    private final HybridRoutingProperties routingProperties;
    private final SubAgentSemanticScorer semanticScorer;

    /**
     * 執行路由決策。
     */
    public SubAgentRoutingDecision route(String inputPayload, ChatModel chatModel, List<SubAgentDescriptor> descriptors) {
        if (descriptors == null || descriptors.isEmpty()) {
            SubAgentDescriptor fallback = defaultDescriptor();
            return new SubAgentRoutingDecision(fallback, 0, 0, 0, true, "no_descriptor");
        }

        Map<String, Double> semanticScores = semanticScorer.score(inputPayload, descriptors, chatModel);
        Set<String> keywords = tokenize(inputPayload);

        double aiWeight = Math.max(0, routingProperties.getAiWeight());
        double keywordWeight = Math.max(0, routingProperties.getKeywordWeight());
        double totalWeight = aiWeight + keywordWeight;
        if (totalWeight <= 0) {
            aiWeight = 0.6;
            keywordWeight = 0.4;
            totalWeight = 1.0;
        }

        final double normalizedAiWeight = aiWeight / totalWeight;
        final double normalizedKeywordWeight = keywordWeight / totalWeight;

        Candidate best = descriptors.stream()
                .map(descriptor -> {
                    double keywordScore = keywordScore(keywords, descriptor);
                    double semanticScore = semanticScores.getOrDefault(descriptor.name(), 0D);
                    double finalScore = normalizedAiWeight * semanticScore + normalizedKeywordWeight * keywordScore;
                    return new Candidate(descriptor, keywordScore, semanticScore, finalScore);
                })
                .max(Comparator
                        .comparingDouble(Candidate::finalScore)
                        .thenComparingDouble(Candidate::semanticScore)
                        .thenComparingDouble(Candidate::keywordScore)
                        .thenComparingInt(c -> c.descriptor().priority())
                        .thenComparing(c -> c.descriptor().name()))
                .orElseGet(() -> new Candidate(defaultDescriptor(), 0, 0, 0));

        double threshold = routingProperties.getThreshold();
        if (best.finalScore() < threshold) {
            SubAgentDescriptor fallback = defaultDescriptor(descriptors);
            return new SubAgentRoutingDecision(
                    fallback,
                    best.keywordScore(),
                    best.semanticScore(),
                    best.finalScore(),
                    true,
                    "below_threshold");
        }

        return new SubAgentRoutingDecision(
                best.descriptor(),
                best.keywordScore(),
                best.semanticScore(),
                best.finalScore(),
                false,
                "matched");
    }

    private double keywordScore(Set<String> keywords, SubAgentDescriptor descriptor) {
        if (keywords.isEmpty()) {
            return 0;
        }

        int matchedName = countTokenMatch(keywords, descriptor.name());
        int matchedDescription = countTokenMatch(keywords, descriptor.description());
        int matchedTools = descriptor.tools() == null ? 0 : descriptor.tools().stream()
                .map(tool -> countTokenMatch(keywords, tool))
                .reduce(0, Integer::sum);

        double nameScore = normalize(matchedName, 2);
        double descriptionScore = normalize(matchedDescription, 4);
        double toolsScore = normalize(matchedTools, 2);

        return (0.4 * nameScore) + (0.4 * descriptionScore) + (0.2 * toolsScore);
    }

    private int countTokenMatch(Set<String> keywords, String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int hit = 0;
        for (String token : keywords) {
            if (token.length() >= 2 && lower.contains(token)) {
                hit++;
            }
        }
        return hit;
    }

    private double normalize(int value, int divisor) {
        if (divisor <= 0) {
            return 0;
        }
        return Math.min(1.0, value / (double) divisor);
    }

    private Set<String> tokenize(String inputPayload) {
        if (inputPayload == null || inputPayload.isBlank()) {
            return Set.of();
        }
        String[] tokens = inputPayload.toLowerCase(Locale.ROOT).split("[^a-zA-Z0-9\\u4e00-\\u9fff]+");
        Set<String> result = new HashSet<>();
        Arrays.stream(tokens)
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .forEach(result::add);
        return result;
    }

    private SubAgentDescriptor defaultDescriptor(List<SubAgentDescriptor> descriptors) {
        return descriptors.stream()
                .filter(descriptor -> "general-researcher".equalsIgnoreCase(descriptor.name()))
                .findFirst()
                .orElseGet(this::defaultDescriptor);
    }

    private SubAgentDescriptor defaultDescriptor() {
        return new SubAgentDescriptor(
                "general-researcher",
                "fallback descriptor",
                List.of(),
                "CLAUDE",
                "agents/subagents/general-researcher.md",
                true,
                0);
    }

    private record Candidate(
            SubAgentDescriptor descriptor,
            double keywordScore,
            double semanticScore,
            double finalScore) {
    }
}
