package com.teamwork.gateway.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 從 markdown frontmatter 載入 sub-agent 描述。
 */
@Component
@Slf4j
public class MarkdownSubAgentDescriptorRepository implements SubAgentDescriptorRepository {

    private static final String RESOURCE_GLOB = "classpath*:agents/subagents/*.md";

    @Override
    public List<SubAgentDescriptor> findEnabledDescriptors() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(RESOURCE_GLOB);
            List<SubAgentDescriptor> descriptors = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                String markdown = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                SubAgentDescriptor descriptor = parseDescriptor(resource, markdown);
                if (descriptor != null && descriptor.enabled()) {
                    descriptors.add(descriptor);
                }
            }
            descriptors.sort(Comparator.comparingInt(SubAgentDescriptor::priority).reversed()
                    .thenComparing(SubAgentDescriptor::name));
            return List.copyOf(descriptors);
        } catch (Exception ex) {
            log.error("Failed to load sub-agent descriptors from markdown", ex);
            return List.of();
        }
    }

    private SubAgentDescriptor parseDescriptor(Resource resource, String markdown) {
        String frontmatter = extractFrontmatter(markdown);
        if (frontmatter.isBlank()) {
            return null;
        }

        String name = "";
        String description = "";
        String ownerProvider = "CLAUDE";
        int priority = 0;
        boolean enabled = true;
        List<String> tools = new ArrayList<>();

        String currentListKey = "";
        for (String rawLine : frontmatter.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("- ") && "tools".equals(currentListKey)) {
                tools.add(line.substring(2).trim());
                continue;
            }

            currentListKey = "";
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }

            String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(separator + 1).trim();
            value = stripQuotes(value);

            switch (key) {
                case "name" -> name = value;
                case "description" -> description = value;
                case "owner-provider", "owner_provider", "provider" -> ownerProvider = value.isBlank() ? "CLAUDE" : value;
                case "priority" -> {
                    try {
                        priority = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                        priority = 0;
                    }
                }
                case "enabled" -> enabled = !"false".equalsIgnoreCase(value);
                case "tools" -> {
                    currentListKey = "tools";
                    if (!value.isBlank() && !"[]".equals(value)) {
                        String normalized = value.replace("[", "").replace("]", "");
                        for (String token : normalized.split(",")) {
                            String tool = token.trim();
                            if (!tool.isEmpty()) {
                                tools.add(stripQuotes(tool));
                            }
                        }
                    }
                }
                default -> {
                }
            }
        }

        if (name.isBlank()) {
            return null;
        }

        String referencePath = "agents/subagents/" + resource.getFilename();
        return new SubAgentDescriptor(name, description, Collections.unmodifiableList(tools), ownerProvider,
                referencePath, enabled, priority);
    }

    private String extractFrontmatter(String markdown) {
        String trimmed = markdown == null ? "" : markdown.trim();
        if (!trimmed.startsWith("---")) {
            return "";
        }

        int first = trimmed.indexOf("---");
        int second = trimmed.indexOf("---", first + 3);
        if (second <= first) {
            return "";
        }
        return trimmed.substring(first + 3, second).trim();
    }

    private String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        String result = value.trim();
        if ((result.startsWith("\"") && result.endsWith("\""))
                || (result.startsWith("'") && result.endsWith("'"))) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }
}
