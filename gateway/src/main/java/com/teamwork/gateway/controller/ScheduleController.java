package com.teamwork.gateway.controller;

import com.teamwork.gateway.dto.ScheduleCreateResponse;
import com.teamwork.gateway.dto.ScheduleListResponse;
import com.teamwork.gateway.dto.ScheduleObservabilityResponse;
import com.teamwork.gateway.dto.ScheduleRunListResponse;
import com.teamwork.gateway.dto.ScheduleRunNowResponse;
import com.teamwork.gateway.dto.ScheduleSummaryResponse;
import com.teamwork.gateway.dto.ScheduleUpsertRequest;
import com.teamwork.gateway.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ScheduleCreateResponse> create(@Valid @RequestBody ScheduleUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(request));
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<ScheduleSummaryResponse> update(
            @PathVariable String scheduleId,
            @Valid @RequestBody ScheduleUpsertRequest request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(scheduleId, request));
    }

    @PatchMapping("/{scheduleId}/enable")
    public ResponseEntity<ScheduleSummaryResponse> enable(@PathVariable String scheduleId) {
        return ResponseEntity.ok(scheduleService.enableSchedule(scheduleId));
    }

    @PatchMapping("/{scheduleId}/disable")
    public ResponseEntity<ScheduleSummaryResponse> disable(@PathVariable String scheduleId) {
        return ResponseEntity.ok(scheduleService.disableSchedule(scheduleId));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(@PathVariable String scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ScheduleListResponse> list(
            @RequestParam String tenantId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String targetType) {
        return ResponseEntity.ok(scheduleService.listSchedules(tenantId, enabled, targetType));
    }

    @PostMapping("/{scheduleId}/run-now")
    public ResponseEntity<ScheduleRunNowResponse> runNow(@PathVariable String scheduleId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(scheduleService.runNow(scheduleId));
    }

    @GetMapping("/{scheduleId}/runs")
    public ResponseEntity<ScheduleRunListResponse> listRuns(@PathVariable String scheduleId) {
        return ResponseEntity.ok(scheduleService.listRuns(scheduleId));
    }

    @GetMapping("/observability")
    public ResponseEntity<ScheduleObservabilityResponse> observability() {
        return ResponseEntity.ok(scheduleService.getObservabilitySnapshot());
    }
}
