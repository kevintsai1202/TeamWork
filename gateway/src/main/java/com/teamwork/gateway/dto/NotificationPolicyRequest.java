package com.teamwork.gateway.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 建立或更新通知策略的請求體。
 */
@Data
@NoArgsConstructor
public class NotificationPolicyRequest {
    /** 租戶 ID */
    private String tenantId;
    /** 策略名稱 */
    private String name;
    /** 任務啟動時通知 */
    private Boolean onStarted;
    /** 任務成功時通知 */
    private Boolean onSuccess;
    /** 任務失敗時通知 */
    private Boolean onFailed;
    /** 任務逾時時通知 */
    private Boolean onTimeout;
    /** 綁定的通道 ID 清單 */
    private List<String> channelIds;
    /** 訊息模板 ID（可選） */
    private String templateId;
}
