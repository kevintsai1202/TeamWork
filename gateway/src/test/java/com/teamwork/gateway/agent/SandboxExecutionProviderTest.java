package com.teamwork.gateway.agent;

import com.teamwork.gateway.exception.GlobalExceptionHandler;
import com.teamwork.gateway.exception.SandboxExecutionFailedException;
import com.teamwork.gateway.exception.SandboxTimeoutException;
import com.teamwork.gateway.exception.UnsupportedLanguageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springaicommunity.sandbox.ExecResult;
import org.springaicommunity.sandbox.Sandbox;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SandboxExecutionProvider 單元測試（T14）。
 * <p>使用 protected createLocalSandbox() 覆寫注入 mock Sandbox，避免需要真實 python3/bash 執行環境。</p>
 * <p>採用 LENIENT strictness 以允許部分防禦性 stub 不一定被呼叫。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SandboxExecutionProviderTest {

    /** 被覆寫 createLocalSandbox 的測試用子類別 */
    private static class TestableSandboxExecutionProvider extends SandboxExecutionProvider {

        /** 要回傳的 mock Sandbox */
        private final Sandbox sandboxToReturn;

        TestableSandboxExecutionProvider(Sandbox sandbox) {
            super(List.of("python", "javascript", "bash"), 65536L);
            this.sandboxToReturn = sandbox;
        }

        @Override
        protected Sandbox createSandbox(
                String sandboxType, String dockerImage, String taskId, String filename, String sourceCode) {
            return sandboxToReturn;
        }
    }

    /** 不注入 mock Sandbox 的標準 provider（用於測試語言驗證）*/
    private SandboxExecutionProvider provider;

    @Mock
    private Sandbox mockSandbox;

    @BeforeEach
    void setUp() {
        provider = new SandboxExecutionProvider(List.of("python", "javascript", "bash"), 65536L);
    }

    // ---- supports() 測試 ----

    @Test
    void supports_AlwaysReturnsFalse() {
        // SandboxExecutionProvider 不透過 AiModel 路由
        assertThat(provider.supports(null)).isFalse();
    }

    // ---- 語言白名單驗證測試 ----

    @Test
    void execute_WithUnsupportedLanguage_ThrowsUnsupportedLanguageException() {
        AgentExecutionContext ctx = buildCtx("ruby", "puts 'hello'", 0L);

        assertThatThrownBy(() -> provider.execute(ctx))
                .isInstanceOf(UnsupportedLanguageException.class)
                .hasMessageContaining("ruby");
    }

    @Test
    void execute_WithBlankLanguage_ThrowsUnsupportedLanguageException() {
        AgentExecutionContext ctx = buildCtx("", "echo hello", 0L);

        assertThatThrownBy(() -> provider.execute(ctx))
                .isInstanceOf(UnsupportedLanguageException.class);
    }

    @Test
    void execute_WithNullLanguage_ThrowsUnsupportedLanguageException() {
        AgentExecutionContext ctx = buildCtx(null, "echo hello", 0L);

        assertThatThrownBy(() -> provider.execute(ctx))
                .isInstanceOf(UnsupportedLanguageException.class);
    }

    // ---- 正常執行測試（mock sandbox）----

    @Test
    void execute_WithSupportedLanguage_ReturnsOutput() throws Exception {
        // Arrange
        ExecResult mockResult = mock(ExecResult.class);
        when(mockResult.exitCode()).thenReturn(0);
        when(mockResult.failed()).thenReturn(false);
        when(mockResult.mergedLog()).thenReturn("Hello, World!");
        when(mockResult.duration()).thenReturn(Duration.ofMillis(100));
        when(mockSandbox.exec(any())).thenReturn(mockResult);

        TestableSandboxExecutionProvider testProvider = new TestableSandboxExecutionProvider(mockSandbox);
        AgentExecutionContext ctx = buildCtx("python", "print('Hello, World!')", 0L);

        // Act
        String result = testProvider.execute(ctx);

        // Assert
        assertThat(result).isEqualTo("Hello, World!");
    }

    @Test
    void execute_WithNonZeroExitCode_ThrowsSandboxExecutionFailedException() throws Exception {
        // Arrange
        ExecResult mockResult = mock(ExecResult.class);
        when(mockResult.exitCode()).thenReturn(1);
        when(mockResult.failed()).thenReturn(true);
        when(mockResult.mergedLog()).thenReturn("Error output");
        when(mockResult.stderr()).thenReturn("NameError: name 'x' is not defined");
        when(mockResult.duration()).thenReturn(Duration.ofMillis(50));
        when(mockSandbox.exec(any())).thenReturn(mockResult);

        TestableSandboxExecutionProvider testProvider = new TestableSandboxExecutionProvider(mockSandbox);
        AgentExecutionContext ctx = buildCtx("python", "print(x)", 0L);

        // Act & Assert
        assertThatThrownBy(() -> testProvider.execute(ctx))
                .isInstanceOf(SandboxExecutionFailedException.class)
                .hasMessageContaining("exitCode=1");
    }

    @Test
    void execute_WithOutputExceedingLimit_ThrowsSandboxExecutionFailedException() throws Exception {
        // Arrange
        ExecResult mockResult = mock(ExecResult.class);
        when(mockResult.exitCode()).thenReturn(0);
        when(mockResult.failed()).thenReturn(false);
        // 超過 100 bytes 的輸出（使用 tiny limit provider）
        when(mockResult.mergedLog()).thenReturn("A".repeat(200));
        when(mockResult.duration()).thenReturn(Duration.ofMillis(200));
        when(mockSandbox.exec(any())).thenReturn(mockResult);

        // 使用 max-output-bytes=100 的 provider（方便測試）
        SandboxExecutionProvider tinyLimitProvider = new SandboxExecutionProvider(
                List.of("python", "javascript", "bash"), 100L) {
            @Override
            protected Sandbox createSandbox(
                    String sandboxType, String dockerImage, String taskId, String filename, String sourceCode) {
                return mockSandbox;
            }
        };
        AgentExecutionContext ctx = buildCtx("python", "print('A' * 200)", 0L);

        // Act & Assert
        assertThatThrownBy(() -> tinyLimitProvider.execute(ctx))
                .isInstanceOf(SandboxExecutionFailedException.class)
                .hasMessageContaining("max size");
    }

    @Test
    void execute_WithTimeoutException_ThrowsSandboxTimeoutException() throws Exception {
        // Arrange：使用含 "timed out" 關鍵字的 RuntimeException 模擬 zt-exec timeout
        when(mockSandbox.exec(any())).thenThrow(new RuntimeException("Process timed out after 500ms"));

        TestableSandboxExecutionProvider testProvider = new TestableSandboxExecutionProvider(mockSandbox);
        AgentExecutionContext ctx = buildCtx("python", "import time; time.sleep(100)", 500L);

        // Act & Assert
        assertThatThrownBy(() -> testProvider.execute(ctx))
                .isInstanceOf(SandboxTimeoutException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    void execute_WithUnexpectedRuntimeException_ThrowsSandboxExecutionFailedException() throws Exception {
        // Arrange：使用不含 timeout 關鍵字的 RuntimeException 模擬意外錯誤（避免 checked exception 限制）
        when(mockSandbox.exec(any())).thenThrow(new RuntimeException("file system read error"));

        TestableSandboxExecutionProvider testProvider = new TestableSandboxExecutionProvider(mockSandbox);
        AgentExecutionContext ctx = buildCtx("bash", "ls", 0L);

        // Act & Assert
        assertThatThrownBy(() -> testProvider.execute(ctx))
                .isInstanceOf(SandboxExecutionFailedException.class)
                .hasMessageContaining("unexpected error");
    }

    // ---- AgentExecutionContext.hasSandboxTimeout() 測試 ----

    @Test
    void agentExecutionContext_hasSandboxTimeout_ReturnsTrueWhenPositive() {
        AgentExecutionContext ctx = buildCtx("python", "print(1)", 5000L);
        assertThat(ctx.hasSandboxTimeout()).isTrue();
    }

    @Test
    void agentExecutionContext_hasSandboxTimeout_ReturnsFalseWhenZero() {
        AgentExecutionContext ctx = buildCtx("python", "print(1)", 0L);
        assertThat(ctx.hasSandboxTimeout()).isFalse();
    }

    // ---- GlobalExceptionHandler sandbox 例外映射測試 ----

    @Test
    void globalExceptionHandler_handleUnsupportedLanguage_Returns400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        UnsupportedLanguageException ex = new UnsupportedLanguageException("ruby");

        ResponseEntity<?> response = handler.handleUnsupportedLanguage(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var body = (com.teamwork.gateway.exception.ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo("UNSUPPORTED_LANGUAGE");
        assertThat(body.getMessage()).contains("ruby");
        assertThat(body.getTraceId()).isNotBlank();
    }

    @Test
    void globalExceptionHandler_handleSandboxTimeout_Returns408() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        SandboxTimeoutException ex = new SandboxTimeoutException(3000L, new RuntimeException("timeout"));

        ResponseEntity<?> response = handler.handleSandboxTimeout(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUEST_TIMEOUT);
        var body = (com.teamwork.gateway.exception.ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo("SANDBOX_TIMEOUT");
        assertThat(body.getMessage()).contains("3000");
        assertThat(body.getTraceId()).isNotBlank();
    }

    @Test
    void globalExceptionHandler_handleSandboxExecutionFailed_Returns422() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        SandboxExecutionFailedException ex = new SandboxExecutionFailedException("Exit code 1", 1);

        ResponseEntity<?> response = handler.handleSandboxExecutionFailed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        var body = (com.teamwork.gateway.exception.ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo("SANDBOX_EXECUTION_FAILED");
        assertThat(body.getMessage()).contains("Exit code 1");
        assertThat(body.getTraceId()).isNotBlank();
    }

    // ---- Exception 類別測試 ----

    @Test
    void unsupportedLanguageException_ContainsLanguageInfo() {
        UnsupportedLanguageException ex = new UnsupportedLanguageException("ruby");
        assertThat(ex.getLanguage()).isEqualTo("ruby");
        assertThat(ex.getMessage()).contains("ruby");
    }

    @Test
    void sandboxTimeoutException_ContainsTimeoutInfo() {
        SandboxTimeoutException ex = new SandboxTimeoutException(5000L, null);
        assertThat(ex.getTimeoutMs()).isEqualTo(5000L);
        assertThat(ex.getMessage()).contains("5000");
    }

    @Test
    void sandboxExecutionFailedException_ContainsExitCode() {
        SandboxExecutionFailedException ex = new SandboxExecutionFailedException("Failed", 2);
        assertThat(ex.getExitCode()).isEqualTo(2);
        assertThat(ex.getMessage()).isEqualTo("Failed");
    }

    @Test
    void sandboxExecutionFailedException_DefaultExitCode_IsMinusOne() {
        SandboxExecutionFailedException ex = new SandboxExecutionFailedException("Output too large");
        assertThat(ex.getExitCode()).isEqualTo(-1);
    }

    // ---- UnifiedAgentRegistry sandbox 路由測試 ----

    @Test
    void unifiedAgentRegistry_resolve_WithSandboxEnabled_ReturnsSandboxProvider() {
        SandboxExecutionProvider sandboxProvider = new SandboxExecutionProvider(
                List.of("python"), 65536L);
        UnifiedAgentProvider aiProvider = mock(UnifiedAgentProvider.class);

        UnifiedAgentRegistry registry = new UnifiedAgentRegistry(
            List.of(aiProvider), sandboxProvider, new AgentProviderCompatibilityLayer());

        AgentExecutionContext ctx = buildCtx("python", "print(1)", 0L); // sandboxEnabled=true by default in buildCtx

        UnifiedAgentProvider result = registry.resolve(ctx);

        assertThat(result).isInstanceOf(SandboxExecutionProvider.class);
    }

    @Test
    void unifiedAgentRegistry_resolve_WithSandboxDisabled_UsesAiModelProvider() {
        SandboxExecutionProvider sandboxProvider = new SandboxExecutionProvider(
                List.of("python"), 65536L);
        UnifiedAgentProvider aiProvider = mock(UnifiedAgentProvider.class);
        when(aiProvider.supports(any())).thenReturn(true);

        UnifiedAgentRegistry registry = new UnifiedAgentRegistry(
            List.of(aiProvider), sandboxProvider, new AgentProviderCompatibilityLayer());

        // sandboxEnabled=false
        AgentExecutionContext ctx = buildCtxNoSandbox();

        UnifiedAgentProvider result = registry.resolve(ctx);

        assertThat(result).isNotInstanceOf(SandboxExecutionProvider.class);
    }

    // ---- 工具方法 ----

    /**
     * 建構沙盒開啟的 AgentExecutionContext
     */
    private AgentExecutionContext buildCtx(String language, String sourceCode, long timeoutMs) {
        return new AgentExecutionContext(
                "test-task-id",
                "test input",
                null, null,
                "test-agent", "", "spring-ai",
                false,
                true,           // sandboxEnabled = true
                language,
                sourceCode,
                timeoutMs,
                "LOCAL",        // sandboxType
                null);          // sandboxDockerImage
    }

    /**
     * 建構沙盒關閉的 AgentExecutionContext
     */
    private AgentExecutionContext buildCtxNoSandbox() {
        com.teamwork.gateway.entity.AiModel aiModel = new com.teamwork.gateway.entity.AiModel();
        aiModel.setProvider("OPENAI");
        return new AgentExecutionContext(
                "test-task-id-2",
                "test input",
                aiModel, null,
                "test-agent", "", "spring-ai",
                false,
                false,          // sandboxEnabled = false
                null,
                null,
                0L,
                null,           // sandboxType
                null);          // sandboxDockerImage
    }
}
