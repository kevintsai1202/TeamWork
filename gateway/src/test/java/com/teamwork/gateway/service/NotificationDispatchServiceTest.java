package com.teamwork.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamwork.gateway.entity.NotificationChannel;
import com.teamwork.gateway.entity.NotificationDelivery;
import com.teamwork.gateway.entity.NotificationPolicy;
import com.teamwork.gateway.repository.NotificationChannelRepository;
import com.teamwork.gateway.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationDispatchServiceTest — 驗證去重、重試退避、EMAIL stub、DLQ 等核心邏輯。
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private NotificationPolicyService notificationPolicyService;

    @Mock
    private NotificationChannelRepository channelRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationDispatchService service;

    // ── dispatchEvent ──────────────────────────────────────────────

    /** dispatch — 策略不存在應靜默跳過（不拋出例外）*/
    @Test
    void dispatchEvent_policyNotFound_shouldSkipSilently() {
        when(notificationPolicyService.findById("pol-x")).thenReturn(Optional.empty());

        service.dispatchEvent("run-1", "src-1", "t1", "pol-x", "ON_SUCCESS",
                java.util.Map.of("runId", "run-1", "scheduleId", "s1", "eventType", "ON_SUCCESS",
                        "status", "SUCCESS", "errorCode", "", "durationMs", "0"));

        verify(deliveryRepository, never()).save(any());
    }

        /** dispatch — 策略不啟用 ON_SUCCESS，應跳過 */
    @Test
    void dispatchEvent_eventNotEnabled_shouldSkip() {
        NotificationPolicy policy = new NotificationPolicy();
        policy.setId("pol-1");
        policy.setOnSuccess(false);
        policy.setOnFailed(false);
        policy.setChannelIdsJson("[\"ch-1\"]");

        when(notificationPolicyService.findById("pol-1")).thenReturn(Optional.of(policy));

        service.dispatchEvent("run-1", "src-1", "t1", "pol-1", "ON_SUCCESS",
                java.util.Map.of("runId", "run-1", "scheduleId", "s1", "eventType", "ON_SUCCESS",
                        "status", "SUCCESS", "errorCode", "", "durationMs", "0"));

        verify(deliveryRepository, never()).save(any());
    }

    /** dispatch — 正常 EMAIL 發送，應儲存 SENT 記錄 */
    @Test
    void dispatchEvent_emailChannel_shouldSaveAndMarkSent() {
        NotificationPolicy policy = new NotificationPolicy();
        policy.setId("pol-2");
        policy.setOnSuccess(true);
        policy.setChannelIdsJson("[\"ch-email\"]");

        NotificationChannel emailCh = new NotificationChannel();
        emailCh.setId("ch-email");
        emailCh.setChannelType("EMAIL");
        emailCh.setEnabled(true);
        emailCh.setEndpointConfigJson("{\"to\":\"user@example.com\"}");

        when(notificationPolicyService.findById("pol-2")).thenReturn(Optional.of(policy));
        when(notificationPolicyService.parseChannelIds(policy)).thenReturn(List.of("ch-email"));
        when(deliveryRepository.findBySourceRefIdAndEventTypeAndChannelIdAndStatusIn(
                anyString(), anyString(), anyString(), anyList()))
                .thenReturn(Optional.empty());
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(channelRepository.findById("ch-email")).thenReturn(Optional.of(emailCh));

        service.dispatchEvent("run-1", "src-1", "t1", "pol-2", "ON_SUCCESS",
                java.util.Map.of("runId", "run-1", "scheduleId", "s1", "eventType", "ON_SUCCESS",
                        "status", "SUCCESS", "errorCode", "", "durationMs", "100"));

        ArgumentCaptor<NotificationDelivery> captor = ArgumentCaptor.forClass(NotificationDelivery.class);
        // 最後一次 save 應為 SENT 狀態
        verify(deliveryRepository, atLeastOnce()).save(captor.capture());
        List<NotificationDelivery> captures = captor.getAllValues();
        boolean hasSent = captures.stream().anyMatch(d -> "SENT".equals(d.getStatus()));
        assertThat(hasSent).isTrue();
    }

    /** dispatch — 重複派送（dedup），應跳過 */
    @Test
    void dispatchEvent_dedupAlreadySent_shouldSkip() {
        NotificationPolicy policy = new NotificationPolicy();
        policy.setId("pol-3");
        policy.setOnSuccess(true);
        policy.setChannelIdsJson("[\"ch-1\"]");

        NotificationDelivery existing = new NotificationDelivery();
        existing.setStatus("SENT");

        when(notificationPolicyService.findById("pol-3")).thenReturn(Optional.of(policy));
        when(notificationPolicyService.parseChannelIds(policy)).thenReturn(List.of("ch-1"));
        when(deliveryRepository.findBySourceRefIdAndEventTypeAndChannelIdAndStatusIn(
                anyString(), anyString(), eq("ch-1"), anyList()))
                .thenReturn(Optional.of(existing));

        service.dispatchEvent("run-1", "src-dup", "t1", "pol-3", "ON_SUCCESS",
                java.util.Map.of("runId", "run-1", "scheduleId", "s1", "eventType", "ON_SUCCESS",
                        "status", "SUCCESS", "errorCode", "", "durationMs", "0"));

        // dedup → 不呼叫 save（跳過）
        verify(deliveryRepository, never()).save(any());
    }

    // ── retryPendingDeliveries ─────────────────────────────────────

    /** retry — 超過最大重試次數應標記 DROPPED */
    @Test
    void retryPendingDeliveries_maxAttemptsReached_shouldDropDelivery() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId("del-1");
        delivery.setAttempt(5); // MAX = 5
        delivery.setRunId("run-1");
        delivery.setChannelId("ch-x");

        when(deliveryRepository.findByStatusAndNextRetryAtBefore(eq("FAILED"), any(LocalDateTime.class)))
                .thenReturn(List.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.retryPendingDeliveries();

        assertThat(delivery.getStatus()).isEqualTo("DROPPED");
        verify(deliveryRepository).save(delivery);
    }

    /** retry — 通道已停用應標記 DROPPED */
    @Test
    void retryPendingDeliveries_channelDisabled_shouldDropDelivery() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId("del-2");
        delivery.setAttempt(1);
        delivery.setChannelId("ch-disabled");
        delivery.setRunId("run-2");

        NotificationChannel disabledCh = new NotificationChannel();
        disabledCh.setId("ch-disabled");
        disabledCh.setEnabled(false);

        when(deliveryRepository.findByStatusAndNextRetryAtBefore(eq("FAILED"), any(LocalDateTime.class)))
                .thenReturn(List.of(delivery));
        when(channelRepository.findById("ch-disabled")).thenReturn(Optional.of(disabledCh));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.retryPendingDeliveries();

        assertThat(delivery.getStatus()).isEqualTo("DROPPED");
    }

    // ── sendDelivery ───────────────────────────────────────────────

    /** sendDelivery — EMAIL stub 應標記 SENT 並發布事件 */
    @Test
    void sendDelivery_emailStub_shouldMarkSentAndPublishEvent() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId("del-3");
        delivery.setAttempt(0);
        delivery.setRunId("run-3");
        delivery.setEventType("ON_SUCCESS");
        delivery.setPayloadSnapshot("hello");

        NotificationChannel emailCh = new NotificationChannel();
        emailCh.setChannelType("EMAIL");
        emailCh.setEndpointConfigJson("{\"to\":\"admin@example.com\"}");

        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.sendDelivery(delivery, emailCh);

        assertThat(delivery.getStatus()).isEqualTo("SENT");
        assertThat(delivery.getAttempt()).isEqualTo(1);
        verify(eventPublisher).publishEvent(any(com.teamwork.gateway.event.NotificationSentEvent.class));
    }
}
