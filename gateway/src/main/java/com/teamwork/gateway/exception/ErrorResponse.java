package com.teamwork.gateway.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 統一 API 錯誤回應格式（T14 重構版）。
 * <p>格式：{code, message, traceId, timestamp}</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    /** 應用層錯誤碼（如 UNSUPPORTED_LANGUAGE, SANDBOX_TIMEOUT, INTERNAL_ERROR 等） */
    private String code;
    /** 人類可讀的錯誤訊息 */
    private String message;
    /** 請求追蹤 ID，用於日誌關聯 */
    private String traceId;
    /** 錯誤發生的時間戳記 */
    private LocalDateTime timestamp;
}
