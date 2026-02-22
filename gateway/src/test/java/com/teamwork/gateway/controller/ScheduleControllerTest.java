package com.teamwork.gateway.controller;

import com.teamwork.gateway.dto.ScheduleCreateResponse;
import com.teamwork.gateway.dto.ScheduleObservabilityResponse;
import com.teamwork.gateway.dto.ScheduleRunNowResponse;
import com.teamwork.gateway.dto.ScheduleUpsertRequest;
import com.teamwork.gateway.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ScheduleController scheduleController;

    @Test
    void create_ShouldReturnCreatedAndBody() {
        ScheduleUpsertRequest request = new ScheduleUpsertRequest();
        request.setTenantId("tenant-a");
        request.setName("daily-job");

        ScheduleCreateResponse serviceResponse = new ScheduleCreateResponse("schedule-1", LocalDateTime.now().plusMinutes(1));
        when(scheduleService.createSchedule(any(ScheduleUpsertRequest.class))).thenReturn(serviceResponse);

        ResponseEntity<ScheduleCreateResponse> response = scheduleController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().scheduleId()).isEqualTo("schedule-1");
    }

    @Test
    void runNow_ShouldReturnAcceptedAndBody() {
        ScheduleRunNowResponse serviceResponse = new ScheduleRunNowResponse("schedule-1", "run-1", "SUCCESS");
        when(scheduleService.runNow("schedule-1")).thenReturn(serviceResponse);

        ResponseEntity<ScheduleRunNowResponse> response = scheduleController.runNow("schedule-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().runId()).isEqualTo("run-1");
    }

    @Test
    void observability_ShouldReturnSnapshot() {
        ScheduleObservabilityResponse snapshot = new ScheduleObservabilityResponse(
                10,
                8,
                2,
                420,
                Map.of("VALIDATION", 1L, "RUNTIME", 1L),
                Map.of("AGENT", 6L, "TOOL", 4L));
        when(scheduleService.getObservabilitySnapshot()).thenReturn(snapshot);

        ResponseEntity<ScheduleObservabilityResponse> response = scheduleController.observability();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().triggeredCount()).isEqualTo(10);
        assertThat(response.getBody().targetTypeCounts()).containsEntry("AGENT", 6L);
    }
}
