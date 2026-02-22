package com.teamwork.gateway.controller;

import com.teamwork.gateway.dto.*;
import com.teamwork.gateway.entity.NotificationDelivery;
import com.teamwork.gateway.service.NotificationChannelService;
import com.teamwork.gateway.service.NotificationDispatchService;
import com.teamwork.gateway.service.NotificationPolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * NotificationControllerTest — 驗證通知 REST 端點的 HTTP 狀態碼與回應結構。
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationChannelService channelService;

    @Mock
    private NotificationPolicyService policyService;

    @Mock
    private NotificationDispatchService dispatchService;

    @InjectMocks
    private NotificationController controller;

    // ── 通道 CRUD ────────────────────────────────────────────────

    /** POST /notifications/channels — 應回傳 201 Created */
    @Test
    void createChannel_shouldReturn201() {
        NotificationChannelResponse resp = new NotificationChannelResponse();
        resp.setId("ch-1");
        resp.setChannelType("WEBHOOK");

        when(channelService.createChannel(any(NotificationChannelRequest.class))).thenReturn(resp);

        ResponseEntity<NotificationChannelResponse> result =
                controller.createChannel(new NotificationChannelRequest());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo("ch-1");
    }

    /** GET /notifications/channels — 應回傳 200 OK */
    @Test
    void listChannels_shouldReturn200() {
        when(channelService.listChannels("t1", null))
                .thenReturn(new NotificationChannelListResponse(List.of()));

        ResponseEntity<NotificationChannelListResponse> result =
                controller.listChannels("t1", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getChannels()).isEmpty();
    }

    /** PATCH /notifications/channels/{id}/enable — 應呼叫 enableChannel 並回傳 200 */
    @Test
    void enableChannel_shouldReturn200() {
        NotificationChannelResponse resp = new NotificationChannelResponse();
        resp.setId("ch-2");
        resp.setEnabled(true);

        when(channelService.enableChannel("ch-2")).thenReturn(resp);

        ResponseEntity<NotificationChannelResponse> result = controller.enableChannel("ch-2");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().isEnabled()).isTrue();
    }

    /** DELETE /notifications/channels/{id} — 應回傳 204 No Content */
    @Test
    void deleteChannel_shouldReturn204() {
        doNothing().when(channelService).deleteChannel("ch-3");

        ResponseEntity<Void> result = controller.deleteChannel("ch-3");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(channelService).deleteChannel("ch-3");
    }

    // ── 策略 CRUD ────────────────────────────────────────────────

    /** POST /notifications/policies — 應回傳 201 Created */
    @Test
    void createPolicy_shouldReturn201() {
        NotificationPolicyResponse resp = new NotificationPolicyResponse();
        resp.setId("pol-1");

        when(policyService.createPolicy(any(NotificationPolicyRequest.class))).thenReturn(resp);

        ResponseEntity<NotificationPolicyResponse> result =
                controller.createPolicy(new NotificationPolicyRequest());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo("pol-1");
    }

    /** DELETE /notifications/policies/{id} — 應回傳 204 */
    @Test
    void deletePolicy_shouldReturn204() {
        doNothing().when(policyService).deletePolicy("pol-2");

        ResponseEntity<Void> result = controller.deletePolicy("pol-2");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(policyService).deletePolicy("pol-2");
    }

    // ── 派送紀錄查詢 ──────────────────────────────────────────────

    /** GET /notifications/deliveries?runId=run-1 — 應回傳 200 及對應清單 */
    @Test
    void listDeliveries_byRunId_shouldReturn200() {
        NotificationDelivery d = new NotificationDelivery();
        d.setId("del-1");
        d.setRunId("run-1");
        d.setStatus("SENT");

        when(dispatchService.findDeliveries("run-1", null)).thenReturn(List.of(d));

        ResponseEntity<NotificationDeliveryListResponse> result =
                controller.listDeliveries("run-1", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTotal()).isEqualTo(1);
        assertThat(result.getBody().getDeliveries().get(0).getStatus()).isEqualTo("SENT");
    }
}
