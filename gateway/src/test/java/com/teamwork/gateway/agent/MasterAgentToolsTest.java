package com.teamwork.gateway.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MasterAgentToolsTest {

    private final MasterAgentTools tools = new MasterAgentTools();

    @Test
    void getCurrentTime_ShouldUseUtcWhenZoneIsBlank() {
        String result = tools.getCurrentTime("   ");

        assertThat(result).contains("Z");
    }

    @Test
    void getCurrentTime_ShouldUseSpecifiedZone() {
        String result = tools.getCurrentTime("Asia/Taipei");

        assertThat(result).contains("+");
    }
}
