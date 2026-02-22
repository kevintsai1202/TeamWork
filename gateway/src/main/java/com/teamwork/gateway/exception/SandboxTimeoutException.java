package com.teamwork.gateway.exception;

/**
 * 當沙盒執行超過設定的 timeout 閾值時拋出。
 * <p>HTTP 映射：408 Request Timeout，錯誤碼：SANDBOX_TIMEOUT</p>
 */
public class SandboxTimeoutException extends RuntimeException {

    /** 設定的超時限制（毫秒） */
    private final long timeoutMs;

    public SandboxTimeoutException(long timeoutMs, Throwable cause) {
        super("Sandbox execution timed out after " + timeoutMs + "ms", cause);
        this.timeoutMs = timeoutMs;
    }

    /** 返回設定的超時限制（毫秒） */
    public long getTimeoutMs() {
        return timeoutMs;
    }
}
