package com.teamwork.gateway.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建立或更新通知通道的請求體。
 */
@Data
@NoArgsConstructor
public class NotificationChannelRequest {
    /** 租戶 ID */
    private String tenantId;
    /** 通道名稱 */
    private String name;
    /** 通道型別：EMAIL / WEBHOOK */
    private String channelType;
    /** 端點設定（JSON 字串） */
    private String endpointConfigJson;
    /** 是否啟用（預設 true） */
    private Boolean enabled;
}
