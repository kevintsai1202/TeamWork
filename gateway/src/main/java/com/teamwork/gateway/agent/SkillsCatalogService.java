package com.teamwork.gateway.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Skills 目錄服務：負責載入、快取與讀取 markdown skills。
 */
@Service
@Slf4j
public class SkillsCatalogService {

    private final PathMatchingResourcePatternResolver resolver;
    private final String resourceGlob;
    private final long cacheTtlMs;

    private volatile long cacheExpireAtMillis = 0L;
    private volatile Map<String, String> cachedSkills = Map.of();

    @Autowired
    public SkillsCatalogService(
            @Value("${gateway.skills.resource-glob:classpath*:agents/skills/*.md}") String resourceGlob,
            @Value("${gateway.skills.cache-ttl-ms:5000}") long cacheTtlMs) {
        this(new PathMatchingResourcePatternResolver(), resourceGlob, cacheTtlMs);
    }

    SkillsCatalogService(PathMatchingResourcePatternResolver resolver, String resourceGlob, long cacheTtlMs) {
        this.resolver = resolver;
        this.resourceGlob = resourceGlob;
        this.cacheTtlMs = cacheTtlMs;
    }

    /**
     * 列出目前可用 skills 名稱。
     */
    public List<String> listSkillNames() {
        return new ArrayList<>(skillsSnapshot().keySet());
    }

    /**
     * 讀取指定 skill 內容。
     */
    public String readSkill(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            throw new IllegalArgumentException("skillName is required");
        }
        String normalized = normalizeSkillName(skillName);
        String content = skillsSnapshot().get(normalized);
        if (content == null) {
            throw new IllegalArgumentException("Skill not found: " + skillName);
        }
        return content;
    }

    /**
     * 立即重新整理快取。
     *
     * @return 重新載入後的 skill 數量
     */
    public int refreshNow() {
        synchronized (this) {
            this.cachedSkills = loadSkillsFromResources();
            this.cacheExpireAtMillis = System.currentTimeMillis() + cacheTtlMs;
            return this.cachedSkills.size();
        }
    }

    private Map<String, String> skillsSnapshot() {
        long now = System.currentTimeMillis();
        if (now < cacheExpireAtMillis) {
            return cachedSkills;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now < cacheExpireAtMillis) {
                return cachedSkills;
            }
            cachedSkills = loadSkillsFromResources();
            cacheExpireAtMillis = now + cacheTtlMs;
            return cachedSkills;
        }
    }

    private Map<String, String> loadSkillsFromResources() {
        try {
            Resource[] resources = resolver.getResources(resourceGlob);
            Map<String, String> skillMap = new LinkedHashMap<>();
            for (Resource resource : resources) {
                if (!resource.exists() || resource.getFilename() == null) {
                    continue;
                }
                String filename = resource.getFilename();
                if (!filename.toLowerCase(Locale.ROOT).endsWith(".md")) {
                    continue;
                }
                String skillName = normalizeSkillName(filename.substring(0, filename.length() - 3));
                String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                if (!skillMap.containsKey(skillName)) {
                    skillMap.put(skillName, content);
                }
            }
            return Map.copyOf(skillMap);
        } catch (Exception e) {
            log.error("Failed to load skills from resources. glob={}", resourceGlob, e);
            return Map.of();
        }
    }

    private String normalizeSkillName(String skillName) {
        return skillName.trim().toLowerCase(Locale.ROOT);
    }
}
