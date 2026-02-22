package com.teamwork.gateway.exception;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 全域例外處理器（T14 重構版）。
 * <p>統一使用 {@link ErrorResponse} 格式：{code, message, traceId, timestamp}。</p>
 * <p>T14 新增：沙盒相關例外映射（UNSUPPORTED_LANGUAGE / SANDBOX_TIMEOUT / SANDBOX_EXECUTION_FAILED）。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- 通用工具 ----

    /**
     * 建立標準化的錯誤回應物件，traceId 優先取 MDC，取不到則產生隨機 UUID。
     */
    private ErrorResponse buildError(String code, String message) {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        return new ErrorResponse(code, message, traceId, LocalDateTime.now());
    }

    // ---- 通用例外 ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Validation failed" : error.getDefaultMessage())
                .orElse("Validation failed");
        return new ResponseEntity<>(buildError("VALIDATION_FAILED", message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ResponseEntity<>(buildError("INVALID_ARGUMENT", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex) {
        return new ResponseEntity<>(
                buildError("INTERNAL_ERROR", "伺服器發生未預期的錯誤: " + ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ---- T14 沙盒相關例外 ----

    /**
     * 沙盒語言不支援 → 400 Bad Request。
     */
    @ExceptionHandler(UnsupportedLanguageException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedLanguage(UnsupportedLanguageException ex) {
        return new ResponseEntity<>(buildError("UNSUPPORTED_LANGUAGE", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    /**
     * 沙盒執行超時 → 408 Request Timeout。
     */
    @ExceptionHandler(SandboxTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleSandboxTimeout(SandboxTimeoutException ex) {
        return new ResponseEntity<>(buildError("SANDBOX_TIMEOUT", ex.getMessage()), HttpStatus.REQUEST_TIMEOUT);
    }

    /**
     * 沙盒執行失敗（exitCode != 0 或輸出超限）→ 422 Unprocessable Entity。
     */
    @ExceptionHandler(SandboxExecutionFailedException.class)
    public ResponseEntity<ErrorResponse> handleSandboxExecutionFailed(SandboxExecutionFailedException ex) {
        return new ResponseEntity<>(
                buildError("SANDBOX_EXECUTION_FAILED", ex.getMessage()),
                HttpStatus.UNPROCESSABLE_ENTITY);
    }
}

