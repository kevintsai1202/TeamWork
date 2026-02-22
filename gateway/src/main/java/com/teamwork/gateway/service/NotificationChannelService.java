package com.teamwork.gateway.service;

import com.teamwork.gateway.dto.NotificationChannelListResponse;
import com.teamwork.gateway.dto.NotificationChannelRequest;
import com.teamwork.gateway.dto.NotificationChannelResponse;
import com.teamwork.gateway.entity.NotificationChannel;
import com.teamwork.gateway.repository.NotificationChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知通道管理服務：負責 CRUD 與啟停操作。
 * 支援 EMAIL / WEBHOOK 通道型別。
 */
@Service
@RequiredArgsConstructor
public class NotificationChannelService {

    /** 支援的通道型別白名單 */
    private static final List<String> SUPPORTED_TYPES = List.of("EMAIL", "WEBHOOK");

    private final NotificationChannelRepository channelRepository;

    /**
     * 建立新的通知通道。
     *
     * @param request 建立請求
     * @return 通道回應 DTO
     */
    public NotificationChannelResponse createChannel(NotificationChannelRequest request) {
        validateRequest(request);
        NotificationChannel channel = new NotificationChannel();
        applyRequest(channel, request);
        return toResponse(channelRepository.save(channel));
    }

    /**
     * 更新通知通道。
     *
     * @param channelId 通道 ID
     * @param request   更新請求
     * @return 更新後的通道回應 DTO
     */
    public NotificationChannelResponse updateChannel(String channelId, NotificationChannelRequest request) {
        NotificationChannel channel = getOrThrow(channelId);
        applyRequest(channel, request);
        return toResponse(channelRepository.save(channel));
    }

    /**
     * 啟用通知通道。
     *
     * @param channelId 通道 ID
     * @return 更新後的通道回應 DTO
     */
    public NotificationChannelResponse enableChannel(String channelId) {
        NotificationChannel channel = getOrThrow(channelId);
        channel.setEnabled(true);
        return toResponse(channelRepository.save(channel));
    }

    /**
     * 停用通知通道。
     *
     * @param channelId 通道 ID
     * @return 更新後的通道回應 DTO
     */
    public NotificationChannelResponse disableChannel(String channelId) {
        NotificationChannel channel = getOrThrow(channelId);
        channel.setEnabled(false);
        return toResponse(channelRepository.save(channel));
    }

    /**
     * 刪除通知通道。
     *
     * @param channelId 通道 ID
     */
    public void deleteChannel(String channelId) {
        NotificationChannel channel = getOrThrow(channelId);
        channelRepository.delete(channel);
    }

    /**
     * 查詢租戶下的通知通道列表。
     *
     * @param tenantId 租戶 ID
     * @param enabled  是否過濾啟用狀態（null 為不過濾）
     * @return 通道列表回應 DTO
     */
    public NotificationChannelListResponse listChannels(String tenantId, Boolean enabled) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        List<NotificationChannel> channels = (enabled == null)
                ? channelRepository.findByTenantId(tenantId)
                : channelRepository.findByTenantIdAndEnabled(tenantId, enabled);
        return new NotificationChannelListResponse(channels.stream().map(this::toResponse).toList());
    }

    /**
     * 依 ID 查詢單一通道。
     *
     * @param channelId 通道 ID
     * @return 通道回應 DTO
     */
    public NotificationChannelResponse getChannel(String channelId) {
        return toResponse(getOrThrow(channelId));
    }

    // ── 私有輔助方法 ─────────────────────────────────────────────

    private NotificationChannel getOrThrow(String channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Notification channel not found: " + channelId));
    }

    /** 驗證建立請求必填欄位 */
    private void validateRequest(NotificationChannelRequest request) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.getChannelType() == null || !SUPPORTED_TYPES.contains(request.getChannelType().toUpperCase())) {
            throw new IllegalArgumentException("channelType must be one of: " + SUPPORTED_TYPES);
        }
    }

    /** 將請求欄位套用到實體 */
    private void applyRequest(NotificationChannel channel, NotificationChannelRequest request) {
        if (request.getTenantId() != null) channel.setTenantId(request.getTenantId());
        if (request.getName() != null) channel.setName(request.getName());
        if (request.getChannelType() != null) channel.setChannelType(request.getChannelType().toUpperCase());
        if (request.getEndpointConfigJson() != null) channel.setEndpointConfigJson(request.getEndpointConfigJson());
        if (request.getEnabled() != null) channel.setEnabled(request.getEnabled());
    }

    /** 將實體轉換為回應 DTO */
    private NotificationChannelResponse toResponse(NotificationChannel channel) {
        return new NotificationChannelResponse(
                channel.getId(),
                channel.getTenantId(),
                channel.getName(),
                channel.getChannelType(),
                channel.getEndpointConfigJson(),
                channel.isEnabled(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}
