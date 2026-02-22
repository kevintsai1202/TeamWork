package com.teamwork.gateway.exception;

/**
 * 當沙盒執行完成但 exitCode != 0，或輸出超過大小上限時拋出。
 * <p>HTTP 映射：422 Unprocessable Entity，錯誤碼：SANDBOX_EXECUTION_FAILED</p>
 */
public class SandboxExecutionFailedException extends RuntimeException {

    /** 沙盒程式的退出碼 */
    private final int exitCode;

    public SandboxExecutionFailedException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public SandboxExecutionFailedException(String message) {
        this(message, -1);
    }

    /** 返回沙盒程式的退出碼，-1 表示非正常退出 */
    public int getExitCode() {
        return exitCode;
    }
}
