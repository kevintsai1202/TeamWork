package com.teamwork.gateway.agent;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillsCatalogServiceTest {

    @Test
    void listSkillNames_ShouldLoadSkillsFromTestResources() {
        SkillsCatalogService service = new SkillsCatalogService(
                new PathMatchingResourcePatternResolver(),
                "classpath*:agents/skills/*.md",
                5000L);

        assertThat(service.listSkillNames()).contains("sample-skill");
    }

    @Test
    void readSkill_ShouldReturnContentWhenSkillExists() {
        SkillsCatalogService service = new SkillsCatalogService(
                new PathMatchingResourcePatternResolver(),
                "classpath*:agents/skills/*.md",
                5000L);

        String content = service.readSkill("sample-skill");

        assertThat(content).contains("Sample Skill");
    }

    @Test
    void readSkill_ShouldThrowWhenSkillDoesNotExist() {
        SkillsCatalogService service = new SkillsCatalogService(
                new PathMatchingResourcePatternResolver(),
                "classpath*:agents/skills/*.md",
                5000L);

        assertThatThrownBy(() -> service.readSkill("not-found"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Skill not found");
    }
}
