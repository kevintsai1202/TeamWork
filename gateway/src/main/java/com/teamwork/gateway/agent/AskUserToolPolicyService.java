package com.teamwork.gateway.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamwork.gateway.entity.ToolConfig;
import com.teamwork.gateway.repository.ToolConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ask-user 工具策略服務：由 DB tool_configs 控制工具開關。
 *
 * <p>規則：</p>
 * <ul>
 *   <li>type 需為 BUILT_IN</li>
 *   <li>name 需為 ask-user-question / ask-user-list-pending / ask-user-submit-answer</li>
 *   <li>configuration.enabled=true 才啟用；未配置預設關閉</li>
 * </ul>
 */
@Service
@Slf4j
public class AskUserToolPolicyService {

    private static final String BUILT_IN_TOOL_TYPE = "BUILT_IN";
    private static final long CACHE_TTL_MS = 5000L;

    // --- MasterAgentTools 預設工具名稱（對應方法名）---
    public static final String TOOL_GET_CURRENT_TIME = "getCurrentTime";
    public static final String TOOL_LIST_AVAILABLE_SKILLS = "listAvailableSkills";
    public static final String TOOL_READ_SKILL_CONTENT = "readSkillContent";
    public static final String TOOL_REFRESH_SKILLS_CACHE = "refreshSkillsCache";
    public static final String TOOL_ASK_QUESTION = "askUserQuestion";
    public static final String TOOL_LIST_PENDING = "listPendingUserQuestions";
    public static final String TOOL_SUBMIT_ANSWER = "submitUserAnswer";

    // 舊 key 相容（避免既有資料失效）
    private static final String LEGACY_TOOL_ASK_QUESTION = "ask-user-question";
    private static final String LEGACY_TOOL_LIST_PENDING = "ask-user-list-pending";
    private static final String LEGACY_TOOL_SUBMIT_ANSWER = "ask-user-submit-answer";

    private final ToolConfigRepository toolConfigRepository;
    private final ObjectMapper objectMapper;

    private volatile long cacheExpireAtMillis = 0L;
    private volatile Map<String, Boolean> cachedEnabledMap = Map.of();

    public AskUserToolPolicyService(ToolConfigRepository toolConfigRepository) {
        this.toolConfigRepository = toolConfigRepository;
        this.objectMapper = new ObjectMapper();
    }

    public boolean canAskQuestion() {
        return isEnabled(TOOL_ASK_QUESTION, false)
                || isEnabled(LEGACY_TOOL_ASK_QUESTION, false);
    }

    public boolean canListPending() {
        return isEnabled(TOOL_LIST_PENDING, false)
                || isEnabled(LEGACY_TOOL_LIST_PENDING, false);
    }

    public boolean canSubmitAnswer() {
        return isEnabled(TOOL_SUBMIT_ANSWER, false)
                || isEnabled(LEGACY_TOOL_SUBMIT_ANSWER, false);
    }

    public boolean canGetCurrentTime() {
        return isEnabled(TOOL_GET_CURRENT_TIME, true);
    }

    public boolean canListAvailableSkills() {
        return isEnabled(TOOL_LIST_AVAILABLE_SKILLS, true);
    }

    public boolean canReadSkillContent() {
        return isEnabled(TOOL_READ_SKILL_CONTENT, true);
    }

    public boolean canRefreshSkillsCache() {
        return isEnabled(TOOL_REFRESH_SKILLS_CACHE, true);
    }

    private boolean isEnabled(String toolName, boolean defaultValue) {
        return enabledMapSnapshot().getOrDefault(toolName, defaultValue);
    }

    private Map<String, Boolean> enabledMapSnapshot() {
        long now = System.currentTimeMillis();
        if (now < cacheExpireAtMillis) {
            return cachedEnabledMap;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now < cacheExpireAtMillis) {
                return cachedEnabledMap;
            }
            cachedEnabledMap = loadEnabledMapFromDb();
            cacheExpireAtMillis = now + CACHE_TTL_MS;
            return cachedEnabledMap;
        }
    }

    private Map<String, Boolean> loadEnabledMapFromDb() {
        Map<String, Boolean> enabledMap = new LinkedHashMap<>();
        List<ToolConfig> configs = toolConfigRepository.findByType(BUILT_IN_TOOL_TYPE);
        for (ToolConfig config : configs) {
            String name = config.getName();
            if (TOOL_GET_CURRENT_TIME.equals(name)
                    || TOOL_LIST_AVAILABLE_SKILLS.equals(name)
                    || TOOL_READ_SKILL_CONTENT.equals(name)
                    || TOOL_REFRESH_SKILLS_CACHE.equals(name)
                    || TOOL_ASK_QUESTION.equals(name)
                    || LEGACY_TOOL_ASK_QUESTION.equals(name)
                    || TOOL_LIST_PENDING.equals(name)
                    || LEGACY_TOOL_LIST_PENDING.equals(name)
                    || TOOL_SUBMIT_ANSWER.equals(name)) {
                // LEGACY_TOOL_SUBMIT_ANSWER 由下行條件涵蓋
                enabledMap.put(name, parseEnabled(config.getConfiguration()));
            } else if (LEGACY_TOOL_SUBMIT_ANSWER.equals(name)) {
                enabledMap.put(name, parseEnabled(config.getConfiguration()));
            }
        }
        return Map.copyOf(enabledMap);
    }

    private boolean parseEnabled(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> configMap = objectMapper.readValue(configuration, new TypeReference<>() {
            });
            Object enabled = configMap.get("enabled");
            return enabled instanceof Boolean flag && flag;
        } catch (Exception e) {
            log.warn("Failed to parse ask-user tool configuration, fallback enabled=false. raw={}", configuration, e);
            return false;
        }
    }
}
