package com.teamwork.gateway.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamwork.gateway.entity.ToolConfig;
import com.teamwork.gateway.repository.ToolConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DynamicToolRegistry {

    private static final long CACHE_TTL_MILLIS = 5000L;
    private static final String BUILT_IN_TOOL_TYPE = "BUILT_IN";

    private final ToolConfigRepository toolConfigRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> localToolCatalog;

    private volatile long cacheExpireAtMillis = 0L;
    private volatile List<String> cachedEnabledToolNames = List.of();

    @Autowired
    public DynamicToolRegistry(ToolConfigRepository toolConfigRepository, ToolCapabilityRegistry toolCapabilityRegistry) {
        this.toolConfigRepository = toolConfigRepository;
        this.objectMapper = new ObjectMapper();
        this.localToolCatalog = toolCapabilityRegistry.toolCatalog();
    }

    /**
     * 測試相容建構子：保留舊簽名，內部轉為 ToolCapabilityRegistry。
     */
    DynamicToolRegistry(ToolConfigRepository toolConfigRepository, MasterAgentTools masterAgentTools) {
        this(toolConfigRepository, ToolCapabilityRegistry.fromMasterAgentTools(masterAgentTools));
    }

    /**
     * 取得目前可用的工具物件集合（每次任務呼叫，內部有短 TTL 快取）。
     */
    public Object[] getActiveToolObjects() {
        List<String> enabledToolNames = getEnabledToolNamesFromCache();

        // 若 DB 尚未配置 BUILT_IN 工具，預設啟用所有本地工具，降低初期導入門檻。
        if (enabledToolNames.isEmpty()) {
            return this.localToolCatalog.values().toArray();
        }

        List<Object> activeTools = new ArrayList<>();
        for (String toolName : enabledToolNames) {
            Object toolObject = this.localToolCatalog.get(toolName);
            if (toolObject != null) {
                activeTools.add(toolObject);
            } else {
                log.warn("Tool '{}' is enabled in DB but no local implementation is registered.", toolName);
            }
        }
        return activeTools.toArray();
    }

    /**
     * 讀取可用工具名稱，優先使用記憶體快取，TTL 到期才回 DB 重新載入。
     */
    public List<String> getEnabledToolNamesFromCache() {
        long now = System.currentTimeMillis();
        if (now < this.cacheExpireAtMillis) {
            return this.cachedEnabledToolNames;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now < this.cacheExpireAtMillis) {
                return this.cachedEnabledToolNames;
            }
            this.cachedEnabledToolNames = loadEnabledToolNamesFromDb();
            this.cacheExpireAtMillis = now + CACHE_TTL_MILLIS;
            return this.cachedEnabledToolNames;
        }
    }

    /**
     * 從 DB 讀取 BUILT_IN 工具設定，依 configuration.enabled 決定是否啟用（預設 true）。
     */
    private List<String> loadEnabledToolNamesFromDb() {
        List<ToolConfig> builtInConfigs = this.toolConfigRepository.findByType(BUILT_IN_TOOL_TYPE);
        List<String> enabledToolNames = new ArrayList<>();
        for (ToolConfig config : builtInConfigs) {
            if (isEnabled(config.getConfiguration())) {
                enabledToolNames.add(config.getName());
            }
        }
        return List.copyOf(enabledToolNames);
    }

    /**
     * 判斷工具開關狀態。configuration 為空或解析失敗時，採預設啟用。
     */
    private boolean isEnabled(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            return true;
        }
        try {
            Map<String, Object> configMap = this.objectMapper.readValue(configuration, new TypeReference<>() {
            });
            Object enabled = configMap.get("enabled");
            return !(enabled instanceof Boolean flag) || flag;
        } catch (Exception e) {
            log.warn("Failed to parse tool configuration json, fallback to enabled=true. raw={}", configuration, e);
            return true;
        }
    }
}

