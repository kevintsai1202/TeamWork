package com.teamwork.gateway.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具能力註冊表：彙整所有 ToolProviderAdapter 的工具描述，形成統一目錄。
 */
@Component
@Slf4j
public class ToolCapabilityRegistry {

    private final Map<String, Object> toolCatalog;

    public ToolCapabilityRegistry(List<ToolProviderAdapter> toolProviders) {
        this.toolCatalog = buildToolCatalog(toolProviders);
    }

    /**
     * 測試用 factory：快速建立只含 MasterAgentTools 的 registry。
     */
    static ToolCapabilityRegistry fromMasterAgentTools(MasterAgentTools masterAgentTools) {
        ToolProviderAdapter localAdapter = () -> List.of(
                new ToolDescriptor("master-agent-tools", "BUILT_IN", "LOCAL", masterAgentTools));
        return new ToolCapabilityRegistry(List.of(localAdapter));
    }

    /**
     * 取得名稱->工具物件目錄（只讀）。
     */
    public Map<String, Object> toolCatalog() {
        return toolCatalog;
    }

    /**
     * 取得全部預設工具物件。
     */
    public Object[] allToolObjects() {
        return toolCatalog.values().toArray();
    }

    private Map<String, Object> buildToolCatalog(List<ToolProviderAdapter> toolProviders) {
        Map<String, Object> catalog = new LinkedHashMap<>();
        for (ToolProviderAdapter provider : toolProviders) {
            for (ToolDescriptor descriptor : provider.listTools()) {
                if (catalog.containsKey(descriptor.name())) {
                    log.warn("Duplicate tool name '{}' found, keeping first registration.", descriptor.name());
                    continue;
                }
                catalog.put(descriptor.name(), descriptor.toolObject());
            }
        }
        return Map.copyOf(catalog);
    }
}
