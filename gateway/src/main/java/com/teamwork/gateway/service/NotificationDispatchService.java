package com.teamwork.gateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamwork.gateway.entity.NotificationChannel;
import com.teamwork.gateway.entity.NotificationDelivery;
import com.teamwork.gateway.entity.NotificationPolicy;
import com.teamwork.gateway.entity.ScheduleRun;
import com.teamwork.gateway.entity.TaskSchedule;
import com.teamwork.gateway.event.NotificationFailedEvent;
import com.teamwork.gateway.event.NotificationSentEvent;
import com.teamwork.gateway.repository.NotificationChannelRepository;
import com.teamwork.gateway.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通知派送引擎：負責將通知事件路由到各通知通道，並提供重試退避與去重保護。
 *
 * 派送流程：
 * 1. 依 policyId 載入策略，確認事件類型是否啟用
 * 2. 遍歷綁定通道，以 (sourceRefId, eventType, channelId) 去重
 * 3. 建立 NotificationDelivery 記錄並嘗試送出
 * 4. 成功 → SENT；失敗 → FAILED + 設定下次重試時間
 * 5. 每 60 秒輪詢 FAILED 紀錄，執行退避重試（最多 5 次後進 DLQ=DROPPED）
 * 6. 派送成功/失敗後發布 SSE 事件
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatchService {

    /** 最大重試次數（超過後標記為 DROPPED，進入 DLQ） */
    private static final int MAX_RETRY_ATTEMPTS = 5;

    /** 退避延遲（分鐘）：1, 2, 4, 8, 16 */
    private static final long[] RETRY_DELAYS_MINUTES = {1, 2, 4, 8, 16};

    /** 預設通知訊息模板（${varName} 格式） */
    private static final String DEFAULT_TEMPLATE =
            "[TeamWork] ${eventType} | run=${runId} | schedule=${scheduleId} | status=${status} | errorCode=${errorCode} | duration=${durationMs}ms";

    private final NotificationPolicyService notificationPolicyService;
    private final NotificationChannelRepository channelRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // RestClient for WEBHOOK dispatch
    private final RestClient restClient = RestClient.create();

    // ── 對外入口 ────────────────────────────────────────────────

    /**
     * 排程執行結束後觸發派送（供 ScheduleService 呼叫）。
     *
     * @param run      執行紀錄
     * @param schedule 對應排程設定
     */
    public void dispatchForScheduleRun(ScheduleRun run, TaskSchedule schedule) {
        if (schedule.getNotificationPolicyId() == null) return;

        // 根據執行結果決定事件類型
        String eventType = "SUCCESS".equals(run.getStatus()) ? "ON_SUCCESS" : "ON_FAILED";
        Map<String, String> context = Map.of(
                "runId", run.getId(),
                "scheduleId", run.getScheduleId(),
                "eventType", eventType,
                "status", run.getStatus() != null ? run.getStatus() : "",
                "errorCode", run.getErrorCode() != null ? run.getErrorCode() : "",
                "durationMs", run.getDurationMs() != null ? String.valueOf(run.getDurationMs()) : "0"
        );
        dispatchEvent(run.getId(), run.getScheduleId(), run.getTenantId(),
                schedule.getNotificationPolicyId(), eventType, context);
    }

    /**
     * 任務執行結束後觸發派送（供 TriggerExecutionService 呼叫）。
     *
     * @param runId        執行 ID（taskId）
     * @param sourceRefId  觸發器 ID
     * @param tenantId     租戶 ID
     * @param policyId     通知策略 ID
     * @param status       執行狀態（SUCCESS / FAILED）
     * @param errorCode    錯誤碼（失敗時）
     */
    public void dispatchForTrigger(String runId, String sourceRefId, String tenantId,
                                    String policyId, String status, String errorCode) {
        if (policyId == null) return;
        String eventType = "SUCCESS".equals(status) ? "ON_SUCCESS" : "ON_FAILED";
        Map<String, String> context = Map.of(
                "runId", runId,
                "scheduleId", sourceRefId != null ? sourceRefId : "",
                "eventType", eventType,
                "status", status != null ? status : "",
                "errorCode", errorCode != null ? errorCode : "",
                "durationMs", "0"
        );
        dispatchEvent(runId, sourceRefId, tenantId, policyId, eventType, context);
    }

    /**
     * 查詢通知派送紀錄列表。
     *
     * @param runId      執行 ID（可選過濾條件）
     * @param tenantId   租戶 ID（可選過濾條件）
     * @return 派送紀錄清單
     */
    public List<NotificationDelivery> findDeliveries(String runId, String tenantId) {
        if (runId != null && !runId.isBlank()) {
            return deliveryRepository.findByRunId(runId);
        }
        if (tenantId != null && !tenantId.isBlank()) {
            return deliveryRepository.findByTenantId(tenantId);
        }
        return List.of();
    }

    // ── 重試排程器 ────────────────────────────────────────────────

    /**
     * 每 60 秒輪詢 FAILED 的派送記錄，執行退避重試。
     * 超過 MAX_RETRY_ATTEMPTS 次後標記為 DROPPED（進入 DLQ）。
     */
    @Scheduled(fixedDelay = 60_000)
    public void retryPendingDeliveries() {
        List<NotificationDelivery> failedDeliveries =
                deliveryRepository.findByStatusAndNextRetryAtBefore("FAILED", LocalDateTime.now());
        for (NotificationDelivery delivery : failedDeliveries) {
            try {
                if (delivery.getAttempt() >= MAX_RETRY_ATTEMPTS) {
                    // 進入 DLQ
                    delivery.setStatus("DROPPED");
                    delivery.setErrorMessage("Max retry attempts (" + MAX_RETRY_ATTEMPTS + ") reached. Delivery dropped (DLQ).");
                    deliveryRepository.save(delivery);
                    log.warn("Delivery dropped (DLQ): deliveryId={}, channelId={}, runId={}",
                            delivery.getId(), delivery.getChannelId(), delivery.getRunId());
                    continue;
                }
                // 取得通道後重試
                Optional<NotificationChannel> channelOpt = channelRepository.findById(delivery.getChannelId());
                if (channelOpt.isEmpty() || !channelOpt.get().isEnabled()) {
                    delivery.setStatus("DROPPED");
                    delivery.setErrorMessage("Channel not found or disabled.");
                    deliveryRepository.save(delivery);
                    continue;
                }
                sendDelivery(delivery, channelOpt.get());
            } catch (Exception ex) {
                log.warn("Retry attempt failed for deliveryId={}: {}", delivery.getId(), ex.getMessage());
            }
        }
    }

    // ── 核心派送邏輯 ──────────────────────────────────────────────

    /**
     * 核心派送流程：載入策略 → 去重 → 建立 Delivery → 發送。
     *
     * @param runId       執行 ID
     * @param sourceRefId 來源識別鍵（scheduleId / triggerId）
     * @param tenantId    租戶 ID
     * @param policyId    通知策略 ID
     * @param eventType   事件類型
     * @param context     訊息模板變數
     */
    void dispatchEvent(String runId, String sourceRefId, String tenantId,
                       String policyId, String eventType, Map<String, String> context) {
        // 載入策略
        Optional<NotificationPolicy> policyOpt = notificationPolicyService.findById(policyId);
        if (policyOpt.isEmpty()) {
            log.warn("Notification policy not found: {}", policyId);
            return;
        }
        NotificationPolicy policy = policyOpt.get();

        // 確認此事件類型是否已在策略中啟用
        if (!isEventEnabled(policy, eventType)) {
            log.debug("Event {} not enabled in policy {}", eventType, policyId);
            return;
        }

        // 取得通道 ID 清單
        List<String> channelIds = notificationPolicyService.parseChannelIds(policy);
        if (channelIds.isEmpty()) {
            log.debug("No channels configured in policy {}", policyId);
            return;
        }

        String messageBody = renderTemplate(policy.getTemplateId(), context);

        for (String channelId : channelIds) {
            // 去重：同一 sourceRefId + eventType + channelId 已存在 SENT 或 PENDING → 跳過
            Optional<NotificationDelivery> existing = deliveryRepository
                    .findBySourceRefIdAndEventTypeAndChannelIdAndStatusIn(
                            sourceRefId, eventType, channelId, List.of("SENT", "PENDING"));
            if (existing.isPresent()) {
                log.debug("Dedup: delivery already present for sourceRefId={}, eventType={}, channelId={}",
                        sourceRefId, eventType, channelId);
                continue;
            }

            // 建立派送記錄
            NotificationDelivery delivery = new NotificationDelivery();
            delivery.setTenantId(tenantId);
            delivery.setRunId(runId);
            delivery.setSourceRefId(sourceRefId);
            delivery.setEventType(eventType);
            delivery.setChannelId(channelId);
            delivery.setStatus("PENDING");
            delivery.setAttempt(0);
            delivery.setPayloadSnapshot(messageBody);
            deliveryRepository.save(delivery);

            // 取得通道實體並發送
            Optional<NotificationChannel> channelOpt = channelRepository.findById(channelId);
            if (channelOpt.isEmpty() || !channelOpt.get().isEnabled()) {
                delivery.setStatus("DROPPED");
                delivery.setErrorMessage("Channel not found or disabled.");
                deliveryRepository.save(delivery);
                continue;
            }
            sendDelivery(delivery, channelOpt.get());
        }
    }

    /**
     * 發送單筆通知（依通道型別分派）。
     *
     * @param delivery 派送記錄
     * @param channel  通知通道
     */
    void sendDelivery(NotificationDelivery delivery, NotificationChannel channel) {
        delivery.setAttempt(delivery.getAttempt() + 1);
        try {
            switch (channel.getChannelType()) {
                case "EMAIL" -> sendEmail(delivery, channel);
                case "WEBHOOK" -> sendWebhook(delivery, channel);
                default -> throw new IllegalArgumentException("Unsupported channel type: " + channel.getChannelType());
            }
            // 成功
            delivery.setStatus("SENT");
            delivery.setErrorMessage(null);
            deliveryRepository.save(delivery);
            eventPublisher.publishEvent(
                    new NotificationSentEvent(delivery.getId(), delivery.getRunId(),
                            channel.getChannelType(), delivery.getEventType()));
            log.info("Notification sent: deliveryId={}, channelType={}, attempt={}",
                    delivery.getId(), channel.getChannelType(), delivery.getAttempt());
        } catch (Exception ex) {
            // 失敗 → 計算下次重試時間
            delivery.setStatus("FAILED");
            delivery.setErrorMessage(ex.getMessage());
            int attemptIndex = Math.min(delivery.getAttempt() - 1, RETRY_DELAYS_MINUTES.length - 1);
            delivery.setNextRetryAt(LocalDateTime.now().plusMinutes(RETRY_DELAYS_MINUTES[attemptIndex]));
            deliveryRepository.save(delivery);
            eventPublisher.publishEvent(
                    new NotificationFailedEvent(delivery.getId(), delivery.getRunId(),
                            channel.getChannelType(), delivery.getAttempt(), ex.getMessage()));
            log.warn("Notification failed: deliveryId={}, channelType={}, attempt={}, error={}",
                    delivery.getId(), channel.getChannelType(), delivery.getAttempt(), ex.getMessage());
        }
    }

    // ── 通道特定發送實作 ─────────────────────────────────────────

    /**
     * EMAIL 通道：MVP 階段僅寫入日誌（框架預留），不實際發送 SMTP。
     *
     * @param delivery 派送記錄
     * @param channel  EMAIL 通道設定
     */
    private void sendEmail(NotificationDelivery delivery, NotificationChannel channel) {
        // MVP：寫入日誌，待後續整合 JavaMailSender
        log.info("[EMAIL STUB] To={} Subject=TeamWork Notification Body={}",
                parseEmailTo(channel.getEndpointConfigJson()),
                delivery.getPayloadSnapshot());
    }

    /**
     * WEBHOOK 通道：HTTP POST JSON 至目標 URL。
     *
     * @param delivery 派送記錄
     * @param channel  WEBHOOK 通道設定
     */
    private void sendWebhook(NotificationDelivery delivery, NotificationChannel channel) {
        String url = parseWebhookUrl(channel.getEndpointConfigJson());
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Webhook URL is missing in endpointConfigJson");
        }
        restClient.post()
                .uri(url)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "deliveryId", delivery.getId(),
                        "runId", delivery.getRunId() != null ? delivery.getRunId() : "",
                        "eventType", delivery.getEventType(),
                        "message", delivery.getPayloadSnapshot() != null ? delivery.getPayloadSnapshot() : ""))
                .retrieve()
                .toBodilessEntity();
    }

    // ── 訊息模板渲染 ──────────────────────────────────────────────

    /**
     * 以 ${varName} 格式將上下文變數代入訊息模板。
     * MVP 使用預設模板；若有 templateId 則加入前綴辨識（後續可擴充為 DB 模板）。
     *
     * @param templateId 模板 ID（可為 null）
     * @param context    變數對應表
     * @return 渲染後的訊息字串
     */
    String renderTemplate(String templateId, Map<String, String> context) {
        String template = DEFAULT_TEMPLATE;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            template = template.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return template;
    }

    // ── 判斷事件是否啟用 ─────────────────────────────────────────

    private boolean isEventEnabled(NotificationPolicy policy, String eventType) {
        return switch (eventType) {
            case "ON_STARTED" -> policy.isOnStarted();
            case "ON_SUCCESS" -> policy.isOnSuccess();
            case "ON_FAILED" -> policy.isOnFailed();
            case "ON_TIMEOUT" -> policy.isOnTimeout();
            default -> false;
        };
    }

    // ── JSON 解析輔助 ─────────────────────────────────────────────

    /**
     * 從 EMAIL endpointConfigJson 解析收件人清單為字串。
     *
     * @param configJson 端點設定 JSON
     * @return 收件人字串（逗號分隔）
     */
    private String parseEmailTo(String configJson) {
        if (configJson == null) return "(unknown)";
        try {
            Map<String, Object> config = objectMapper.readValue(configJson, new TypeReference<>() {});
            Object to = config.get("to");
            return to != null ? to.toString() : "(unknown)";
        } catch (Exception e) {
            return "(parse-error)";
        }
    }

    /**
     * 從 WEBHOOK endpointConfigJson 解析目標 URL。
     *
     * @param configJson 端點設定 JSON
     * @return URL 字串（找不到時回 null）
     */
    private String parseWebhookUrl(String configJson) {
        if (configJson == null) return null;
        try {
            Map<String, Object> config = objectMapper.readValue(configJson, new TypeReference<>() {});
            Object url = config.get("url");
            return url != null ? url.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
