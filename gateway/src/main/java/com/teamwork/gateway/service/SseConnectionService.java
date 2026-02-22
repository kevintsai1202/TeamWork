package com.teamwork.gateway.service;

import com.teamwork.gateway.event.ContextCompressedEvent;
import com.teamwork.gateway.event.NotificationFailedEvent;
import com.teamwork.gateway.event.NotificationSentEvent;
import com.teamwork.gateway.event.TaskStatusChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseConnectionService {

    // 存放正在監聽的 SseEmitter
    // <taskId, SseEmitter>
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 建立特定 TaskID 的 SSE 連線，並處理 Timeout 以及 Completion 事件。
     *
     * @param taskId 任務的 UUID
     * @return 該任務註冊好的 SseEmitter
     */
    public SseEmitter subscribe(String taskId) {
        // 設定 30 分鐘 Timeout (1000 * 60 * 30 ms)
        SseEmitter emitter = new SseEmitter(1800000L);
        emitters.put(taskId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE Connection completed for taskId: {}", taskId);
            emitters.remove(taskId);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE Connection timeout for taskId: {}", taskId);
            emitters.remove(taskId);
        });
        emitter.onError((e) -> {
            log.error("SSE Connection error for taskId: {}", taskId, e);
            emitters.remove(taskId);
        });

        log.info("Client subscribed to SSE for taskId: {}", taskId);
        return emitter;
    }

    /**
     * 從整個 Spring ApplicationContext 中接收 TaskStatusChangeEvent，並分派給對應的 SseEmitter。
     */
    @EventListener
    public void handleTaskStatusChange(TaskStatusChangeEvent event) {
        String taskId = event.getTaskId();
        SseEmitter emitter = emitters.get(taskId);

        if (emitter != null) {
            try {
                // 送出 JSON 格式 { "status": "RUNNING", "payload": "..." }
                // 這邊利用 SseEmitter.event() 的輔助方法建構格式
                SseEmitter.SseEventBuilder sseEvent = SseEmitter.event()
                        .id(taskId)
                        .name("task-status")
                        .data(Map.of(
                                "status", event.getStatus(),
                                "payload", event.getPayload() != null ? event.getPayload() : ""));
                emitter.send(sseEvent);
                log.info("Dispatched SSE event for taskId: {} with status: {}", taskId, event.getStatus());

                // 若任務結束，主動關閉這個 emitter
                if ("COMPLETED".equals(event.getStatus()) || "FAILED".equals(event.getStatus())) {
                    emitter.complete();
                    emitters.remove(taskId); // onCompletion 也會呼叫，不過提早刪除無妨
                }
            } catch (IOException e) {
                log.error("Failed to send SSE event to taskId: {}", taskId, e);
                // 送不出的話直接讓他 complete 斷線，稍後由 timeout / error 自動清除，或是手動清
                emitters.remove(taskId);
            }
        }
    }

    @EventListener
    public void handleContextCompressed(ContextCompressedEvent event) {
        String taskId = event.getTaskId();
        SseEmitter emitter = emitters.get(taskId);
        if (emitter == null) {
            return;
        }
        try {
            SseEmitter.SseEventBuilder sseEvent = SseEmitter.event()
                    .id(taskId)
                    .name("context.compressed")
                    .data(Map.of(
                            "taskId", taskId,
                            "beforeTokens", event.getBeforeTokens(),
                            "afterTokens", event.getAfterTokens(),
                            "savedTokens", event.getSavedTokens(),
                            "savedRatio", event.getSavedRatio()));
            emitter.send(sseEvent);
            log.info("Dispatched context.compressed SSE for taskId: {}", taskId);
        } catch (IOException e) {
            log.error("Failed to send context.compressed SSE to taskId: {}", taskId, e);
            emitters.remove(taskId);
        }
    }

    /**
     * 監聽通知派送成功事件，廣播 notification.sent SSE 給對應 runId 的訂閱者。
     */
    @EventListener
    public void handleNotificationSent(NotificationSentEvent event) {
        SseEmitter emitter = emitters.get(event.runId());
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .id(event.runId())
                    .name("notification.sent")
                    .data(Map.of(
                            "deliveryId", event.deliveryId(),
                            "runId", event.runId(),
                            "channelType", event.channelType(),
                            "eventType", event.eventType())));
        } catch (IOException e) {
            log.error("Failed to send notification.sent SSE for runId: {}", event.runId(), e);
            emitters.remove(event.runId());
        }
    }

    /**
     * 監聽通知派送失敗事件，廣播 notification.failed SSE 給對應 runId 的訂閱者。
     */
    @EventListener
    public void handleNotificationFailed(NotificationFailedEvent event) {
        SseEmitter emitter = emitters.get(event.runId());
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .id(event.runId())
                    .name("notification.failed")
                    .data(Map.of(
                            "deliveryId", event.deliveryId(),
                            "runId", event.runId(),
                            "channelType", event.channelType(),
                            "attempt", event.attempt(),
                            "error", event.error() != null ? event.error() : "")));
        } catch (IOException e) {
            log.error("Failed to send notification.failed SSE for runId: {}", event.runId(), e);
            emitters.remove(event.runId());
        }
    }
}
