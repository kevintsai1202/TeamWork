package com.teamwork.gateway.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentObservabilityServiceTest {

    private final AgentObservabilityService service = new AgentObservabilityService();

    @Test
    void snapshot_ShouldAggregateStartedCompletedFailedAndDuration() {
        service.recordTaskStarted("t-1");
        service.recordTaskCompleted("t-1", 120);

        service.recordTaskStarted("t-2");
        service.recordTaskFailed("t-2", new IllegalArgumentException("bad request"));

        AgentObservabilitySnapshot snapshot = service.snapshot();

        assertThat(snapshot.started()).isEqualTo(2);
        assertThat(snapshot.completed()).isEqualTo(1);
        assertThat(snapshot.failed()).isEqualTo(1);
        assertThat(snapshot.averageDurationMs()).isEqualTo(120);
        assertThat(snapshot.failedByCategory()).containsEntry("VALIDATION", 1L);
    }

    @Test
    void classifyError_ShouldReturnExpectedCategory() {
        assertThat(service.classifyError(new IllegalArgumentException("x"))).isEqualTo("VALIDATION");
        assertThat(service.classifyError(new IllegalStateException("x"))).isEqualTo("CONFIGURATION");
        assertThat(service.classifyError(new RuntimeException("timeout while calling model"))).isEqualTo("TIMEOUT");
        assertThat(service.classifyError(new RuntimeException("random runtime"))).isEqualTo("RUNTIME");
        assertThat(service.classifyError(new Exception("x"))).isEqualTo("INTERNAL");
        assertThat(service.classifyError(null)).isEqualTo("UNKNOWN");
    }
}
