package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import com.teamwork.gateway.exception.SandboxExecutionFailedException;
import com.teamwork.gateway.exception.UnsupportedLanguageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * DockerSandbox 整合測試（T15-Docker）。
 *
 * <p>使用真實 DockerSandbox（Testcontainers 後端）在隔離容器中執行程式碼。
 * 標記為 {@code @Tag("docker")}，預設 CI 環境可透過
 * {@code -Dgroups='!docker'} 跳過；有 Docker daemon 時才執行。</p>
 *
 * <p>執行方式：</p>
 * <pre>{@code
 * # 只跑 Docker 標籤
 * ./mvnw test -Dgroups=docker
 * # 跳過 Docker 標籤（CI 無 daemon 時）
 * ./mvnw test -Dgroups='!docker'
 * }</pre>
 */
@Tag("docker")
class SandboxDockerIntegrationTest {

    /** 使用預設 Docker image（agents-runtime）的 Provider */
    private SandboxExecutionProvider provider;

    /** 建立 3-param 建構子的 Provider，直接使用預設 Docker image */
    @BeforeEach
    void setUp() {
        provider = new SandboxExecutionProvider(
                List.of("python", "javascript", "bash"),
                65536L,
                "ghcr.io/spring-ai-community/agents-runtime:latest");
    }

    /**
     * 建立測試用 context（DOCKER 沙盒類型）的輔助方法。
     *
     * @param language   程式語言
     * @param sourceCode 原始碼
     * @param timeoutMs  0 代表不設 timeout
     * @param dockerImage null 使用 Provider 預設，否則指定映像
     */
    private AgentExecutionContext buildDockerCtx(
            String language, String sourceCode, long timeoutMs, String dockerImage) {
        AiModel aiModel = new AiModel();
        aiModel.setProvider("OPENAI");
        return new AgentExecutionContext(
                "docker-task-1", "payload", aiModel, null,
                "sandbox-agent", "", "sandbox", false,
                true, language, sourceCode, timeoutMs,
                "DOCKER", dockerImage);
    }

    // =================== Python 測試 ===================

    @Test
    void docker_Python_HelloWorld_ReturnsOutput() {
        // Arrange
        AgentExecutionContext ctx = buildDockerCtx("python", "print('hello from docker')", 60000L, null);

        // Act
        String output = provider.execute(ctx);

        // Assert
        assertThat(output).containsIgnoringCase("hello from docker");
    }

    @Test
    void docker_Python_Arithmetic_CorrectResult() {
        // Arrange
        AgentExecutionContext ctx = buildDockerCtx("python", "print(2 + 3)", 60000L, null);

        // Act
        String output = provider.execute(ctx);

        // Assert
        assertThat(output.trim()).isEqualTo("5");
    }

    @Test
    void docker_Python_RuntimeError_ThrowsException() {
        // Arrange — 故意執行會失敗的程式碼
        AgentExecutionContext ctx = buildDockerCtx("python", "raise ValueError('docker error test')", 60000L, null);

        // Act & Assert
        assertThatThrownBy(() -> provider.execute(ctx))
                .isInstanceOf(SandboxExecutionFailedException.class);
    }

    // =================== Bash 測試 ===================

    @Test
    void docker_Bash_EchoCommand_ReturnsOutput() {
        // Arrange
        AgentExecutionContext ctx = buildDockerCtx("bash", "echo 'bash in docker works'", 60000L, null);

        // Act
        String output = provider.execute(ctx);

        // Assert
        assertThat(output).containsIgnoringCase("bash in docker works");
    }

    @Test
    void docker_Bash_EnvIsolation_NoBoundaryLeak() {
        // Arrange — 在 Docker 容器內，HOST_SECRETS 這類環境變數應不存在
        AgentExecutionContext ctx = buildDockerCtx(
                "bash",
                "echo \"HOME=${HOME}\" && echo 'isolated'",
                60000L, null);

        // Act
        String output = provider.execute(ctx);

        // Assert — 容器應回傳 isolated 字串，且 HOME 非 Windows 路徑
        assertThat(output).contains("isolated");
        assertThat(output).doesNotContain("C:\\");
    }

    // =================== 語言白名單測試（Docker 路徑依然生效）===================

    @Test
    void docker_UnsupportedLanguage_ThrowsUnsupportedLanguageException() {
        // Arrange — ruby 不在白名單
        AgentExecutionContext ctx = buildDockerCtx("ruby", "puts 'hello'", 60000L, null);

        // Act & Assert
        assertThatThrownBy(() -> provider.execute(ctx))
                .isInstanceOf(UnsupportedLanguageException.class);
    }
}
