package com.teamwork.gateway.controller;

import com.teamwork.gateway.dto.NotificationChannelListResponse;
import com.teamwork.gateway.dto.NotificationChannelRequest;
import com.teamwork.gateway.dto.NotificationChannelResponse;
import com.teamwork.gateway.dto.NotificationDeliveryItem;
import com.teamwork.gateway.dto.NotificationDeliveryListResponse;
import com.teamwork.gateway.dto.NotificationPolicyListResponse;
import com.teamwork.gateway.dto.NotificationPolicyRequest;
import com.teamwork.gateway.dto.NotificationPolicyResponse;
import com.teamwork.gateway.entity.NotificationDelivery;
import com.teamwork.gateway.service.NotificationChannelService;
import com.teamwork.gateway.service.NotificationDispatchService;
import com.teamwork.gateway.service.NotificationPolicyService;
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

import java.util.List;

/**
 * 通知編排 API 控制器（T21）。
 * 提供通知通道管理、通知策略管理以及派送紀錄查詢功能。
 *
 * Base URL：/notifications
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationChannelService channelService;
    private final NotificationPolicyService policyService;
    private final NotificationDispatchService dispatchService;

    // ── 通知通道管理（T21-A2） ────────────────────────────────────

    /**
     * POST /notifications/channels — 建立通知通道
     */
    @PostMapping("/channels")
    public ResponseEntity<NotificationChannelResponse> createChannel(
            @RequestBody NotificationChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(channelService.createChannel(request));
    }

    /**
     * GET /notifications/channels — 查詢通知通道列表
     */
    @GetMapping("/channels")
    public ResponseEntity<NotificationChannelListResponse> listChannels(
            @RequestParam String tenantId,
            @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(channelService.listChannels(tenantId, enabled));
    }

    /**
     * GET /notifications/channels/{channelId} — 查詢單一通道
     */
    @GetMapping("/channels/{channelId}")
    public ResponseEntity<NotificationChannelResponse> getChannel(@PathVariable String channelId) {
        return ResponseEntity.ok(channelService.getChannel(channelId));
    }

    /**
     * PUT /notifications/channels/{channelId} — 更新通知通道
     */
    @PutMapping("/channels/{channelId}")
    public ResponseEntity<NotificationChannelResponse> updateChannel(
            @PathVariable String channelId,
            @RequestBody NotificationChannelRequest request) {
        return ResponseEntity.ok(channelService.updateChannel(channelId, request));
    }

    /**
     * PATCH /notifications/channels/{channelId}/enable — 啟用通知通道
     */
    @PatchMapping("/channels/{channelId}/enable")
    public ResponseEntity<NotificationChannelResponse> enableChannel(@PathVariable String channelId) {
        return ResponseEntity.ok(channelService.enableChannel(channelId));
    }

    /**
     * PATCH /notifications/channels/{channelId}/disable — 停用通知通道
     */
    @PatchMapping("/channels/{channelId}/disable")
    public ResponseEntity<NotificationChannelResponse> disableChannel(@PathVariable String channelId) {
        return ResponseEntity.ok(channelService.disableChannel(channelId));
    }

    /**
     * DELETE /notifications/channels/{channelId} — 刪除通知通道
     */
    @DeleteMapping("/channels/{channelId}")
    public ResponseEntity<Void> deleteChannel(@PathVariable String channelId) {
        channelService.deleteChannel(channelId);
        return ResponseEntity.noContent().build();
    }

    // ── 通知策略管理（T21-A3） ────────────────────────────────────

    /**
     * POST /notifications/policies — 建立通知策略
     */
    @PostMapping("/policies")
    public ResponseEntity<NotificationPolicyResponse> createPolicy(
            @RequestBody NotificationPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createPolicy(request));
    }

    /**
     * GET /notifications/policies — 查詢通知策略列表
     */
    @GetMapping("/policies")
    public ResponseEntity<NotificationPolicyListResponse> listPolicies(@RequestParam String tenantId) {
        return ResponseEntity.ok(policyService.listPolicies(tenantId));
    }

    /**
     * GET /notifications/policies/{policyId} — 查詢單一策略
     */
    @GetMapping("/policies/{policyId}")
    public ResponseEntity<NotificationPolicyResponse> getPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(policyService.getPolicy(policyId));
    }

    /**
     * PUT /notifications/policies/{policyId} — 更新通知策略
     */
    @PutMapping("/policies/{policyId}")
    public ResponseEntity<NotificationPolicyResponse> updatePolicy(
            @PathVariable String policyId,
            @RequestBody NotificationPolicyRequest request) {
        return ResponseEntity.ok(policyService.updatePolicy(policyId, request));
    }

    /**
     * DELETE /notifications/policies/{policyId} — 刪除通知策略
     */
    @DeleteMapping("/policies/{policyId}")
    public ResponseEntity<Void> deletePolicy(@PathVariable String policyId) {
        policyService.deletePolicy(policyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /notifications/schedules/{scheduleId}/policy — 為排程綁定通知策略
     * （此功能透過更新排程的 notificationPolicyId 欄位達成，由 ScheduleController 處理）
     * 此端點提供快捷語義綁定。
     */
    @PutMapping("/schedules/{scheduleId}/policy")
    public ResponseEntity<Void> bindPolicyToSchedule(
            @PathVariable String scheduleId,
            @RequestBody java.util.Map<String, String> body) {
        // 由 ScheduleService 負責更新 notificationPolicyId，此端點為語義代理
        // 實際修改排程由前端主動呼叫 PUT /schedules/{scheduleId} 完成
        return ResponseEntity.accepted().build();
    }

    // ── 通知派送紀錄查詢（T21-A4） ────────────────────────────────

    /**
     * GET /notifications/deliveries — 查詢派送紀錄
     * Query: runId（可選）、tenantId（可選）
     */
    @GetMapping("/deliveries")
    public ResponseEntity<NotificationDeliveryListResponse> listDeliveries(
            @RequestParam(required = false) String runId,
            @RequestParam(required = false) String tenantId) {
        List<NotificationDelivery> raw = dispatchService.findDeliveries(runId, tenantId);
        List<NotificationDeliveryItem> items = raw.stream().map(d -> new NotificationDeliveryItem(
                d.getId(), d.getRunId(), d.getSourceRefId(), d.getEventType(),
                d.getChannelId(), d.getStatus(), d.getAttempt(),
                d.getNextRetryAt(), d.getErrorMessage(), d.getCreatedAt()
        )).toList();
        return ResponseEntity.ok(new NotificationDeliveryListResponse(items, items.size()));
    }
}
