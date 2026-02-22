package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.NotificationChannelRequest;
import com.teamwork.gateway.dto.NotificationChannelResponse;
import com.teamwork.gateway.entity.NotificationChannel;
import com.teamwork.gateway.repository.NotificationChannelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotificationChannelServiceTest — 驗證通知頻道 CRUD 與狀態切換邏輯。
 */
@ExtendWith(MockitoExtension.class)
class NotificationChannelServiceTest {

    @Mock
    private NotificationChannelRepository channelRepository;

    @InjectMocks
    private NotificationChannelService service;

    /** 建立頻道 — 正常 WEBHOOK 型別，應成功儲存並回傳 response */
    @Test
    void createChannel_webhook_shouldSaveAndReturnResponse() {
        NotificationChannelRequest req = new NotificationChannelRequest();
        req.setName("my-webhook");
        req.setChannelType("WEBHOOK");
        req.setEndpointConfigJson("{\"url\":\"https://example.com/hook\"}");
        req.setEnabled(true);
        req.setTenantId("t1");

        NotificationChannel saved = new NotificationChannel();
        saved.setId("ch-1");
        saved.setName("my-webhook");
        saved.setChannelType("WEBHOOK");
        saved.setEnabled(true);
        saved.setTenantId("t1");
        saved.setEndpointConfigJson("{\"url\":\"https://example.com/hook\"}");

        when(channelRepository.save(any())).thenReturn(saved);

        NotificationChannelResponse resp = service.createChannel(req);

        assertThat(resp.getId()).isEqualTo("ch-1");
        assertThat(resp.getChannelType()).isEqualTo("WEBHOOK");
        verify(channelRepository).save(any(NotificationChannel.class));
    }

    /** 建立頻道 — 非法 channelType 應拋出例外 */
    @Test
    void createChannel_invalidType_shouldThrow() {
        NotificationChannelRequest req = new NotificationChannelRequest();
        req.setName("bad");
        req.setChannelType("SMS");
        req.setTenantId("t1");

        assertThatThrownBy(() -> service.createChannel(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("channelType must be one of");
    }

    /** 啟用頻道 — 應將 enabled 設為 true 並儲存 */
    @Test
    void enableChannel_shouldSetEnabledTrue() {
        NotificationChannel ch = new NotificationChannel();
        ch.setId("ch-2");
        ch.setEnabled(false);
        ch.setTenantId("t1");

        when(channelRepository.findById("ch-2")).thenReturn(Optional.of(ch));
        when(channelRepository.save(any())).thenReturn(ch);

        service.enableChannel("ch-2");

        assertThat(ch.isEnabled()).isTrue();
        verify(channelRepository).save(ch);
    }

    /** 停用頻道 — 應將 enabled 設為 false 並儲存 */
    @Test
    void disableChannel_shouldSetEnabledFalse() {
        NotificationChannel ch = new NotificationChannel();
        ch.setId("ch-3");
        ch.setEnabled(true);
        ch.setTenantId("t1");

        when(channelRepository.findById("ch-3")).thenReturn(Optional.of(ch));
        when(channelRepository.save(any())).thenReturn(ch);

        service.disableChannel("ch-3");

        assertThat(ch.isEnabled()).isFalse();
        verify(channelRepository).save(ch);
    }

    /** 列出頻道 — 不帶 enabled filter，應呼叫 findByTenantId */
    @Test
    void listChannels_noFilter_shouldCallFindByTenantId() {
        when(channelRepository.findByTenantId("t1")).thenReturn(List.of());

        var result = service.listChannels("t1", null);

        assertThat(result.getChannels()).isEmpty();
        verify(channelRepository).findByTenantId("t1");
    }

    /** 取得單一頻道 — 找不到應拋出例外 */
    @Test
    void getChannel_notFound_shouldThrow() {
        when(channelRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getChannel("x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 刪除頻道 — 應呼叫 deleteById */
    @Test
    void deleteChannel_shouldCallDeleteById() {
        NotificationChannel ch = new NotificationChannel();
        ch.setId("ch-4");
        ch.setTenantId("t1");

        when(channelRepository.findById("ch-4")).thenReturn(Optional.of(ch));

        service.deleteChannel("ch-4");

        verify(channelRepository).delete(any(NotificationChannel.class));
    }
}
