package com.teamwork.gateway.exception;

/**
 * 當沙盒請求的語言不在白名單範圍時拋出。
 * <p>HTTP 映射：400 Bad Request，錯誤碼：UNSUPPORTED_LANGUAGE</p>
 */
public class UnsupportedLanguageException extends RuntimeException {

    /** 被拒絕的語言名稱 */
    private final String language;

    public UnsupportedLanguageException(String language) {
        super("Sandbox language not supported: " + language);
        this.language = language;
    }

    /** 返回被拒絕的語言名稱 */
    public String getLanguage() {
        return language;
    }
}
