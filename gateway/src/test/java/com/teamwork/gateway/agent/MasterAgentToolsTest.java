package com.teamwork.gateway.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void getCurrentTime_WhenDisabledByPolicy_ShouldReturnDisabledMessage() {
        AskUserToolPolicyService policyService = mock(AskUserToolPolicyService.class);
        when(policyService.canGetCurrentTime()).thenReturn(false);
        MasterAgentTools toolWithPolicy = new MasterAgentTools(null, null, null, null, policyService);

        String result = toolWithPolicy.getCurrentTime("UTC");

        assertThat(result).isEqualTo("getCurrentTime tool is disabled");
    }
}
