package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import com.teamwork.gateway.exception.SandboxExecutionFailedException;
import com.teamwork.gateway.exception.SandboxTimeoutException;
import com.teamwork.gateway.exception.UnsupportedLanguageException;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.sandbox.ExecSpec;
import org.springaicommunity.sandbox.LocalSandbox;
import org.springaicommunity.sandbox.Sandbox;
import org.springaicommunity.sandbox.docker.DockerSandbox;
import org.springaicommunity.sandbox.e2b.E2BSandbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 沙盒執行 Provider，使用 agent-sandbox-core 的 LocalSandbox 在受控環境中執行程式碼。
 * <p>此 Provider 不依 AiModel 路由（supports 永遠回傳 false），
 * 而是由 UnifiedAgentRegistry 在 AgentProfile.sandboxEnabled = true 時優先選用。</p>
 * <p>MVP 採用 LocalSandbox（無隔離，直接在主機）；Docker/E2B 隔離為後續版本。</p>
 */
@Component
@Order(1)
@Slf4j
public class SandboxExecutionProvider implements UnifiedAgentProvider {

    /** 允許在沙盒中執行的語言白名單 */
    private final Set<String> allowedLanguages;

    /** 輸出大小上限（bytes），防止過大的輸出淹沒記憶體 */
    private final long maxOutputBytes;

    /** Docker 沙盒預設映像（可由 AgentProfile.dockerImage 覆寫） */
    private final String dockerDefaultImage;

    /** E2B API Key（優先從 application.yml 取得；空值時 fallback 至 E2B_API_KEY 環境變數） */
    private final String e2bApiKey;

    /** Spring 注入建構子（4 params，含 Docker 預設 image 與 E2B API Key） */
    @Autowired
    public SandboxExecutionProvider(
            @Value("${sandbox.allowed-languages:python,javascript,bash}") List<String> allowedLanguages,
            @Value("${sandbox.max-output-bytes:65536}") long maxOutputBytes,
            @Value("${sandbox.docker.default-image:ghcr.io/spring-ai-community/agents-runtime:latest}") String dockerDefaultImage,
            @Value("${sandbox.e2b.api-key:}") String e2bApiKey) {
        this.allowedLanguages = allowedLanguages.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        this.maxOutputBytes = maxOutputBytes;
        this.dockerDefaultImage = dockerDefaultImage;
        this.e2bApiKey = e2bApiKey;
    }

    /** 測試用建構子（3 params，e2bApiKey 空字串、dockerDefaultImage 使用預設值） */
    SandboxExecutionProvider(List<String> allowedLanguages, long maxOutputBytes, String dockerDefaultImage) {
        this(allowedLanguages, maxOutputBytes, dockerDefaultImage, "");
    }

    /** 測試用建構子（2 params，dockerDefaultImage 與 e2bApiKey 均使用預設值） */
    SandboxExecutionProvider(List<String> allowedLanguages, long maxOutputBytes) {
        this(allowedLanguages, maxOutputBytes,
                "ghcr.io/spring-ai-community/agents-runtime:latest", "");
    }

    /**
     * SandboxExecutionProvider 不透過 supports(AiModel) 路由，
     * 路由邏輯完全由 UnifiedAgentRegistry.resolve(AgentExecutionContext) 控制。
     */
    @Override
    public boolean supports(AiModel aiModel) {
        return false;
    }

    /**
     * 執行沙盒任務主流程：
     * 1. 語言白名單檢查
     * 2. 建立 LocalSandbox 並預置原始碼
     * 3. 依語言選擇執行命令
     * 4. 套用 timeout 設定（如有）
     * 5. 檢查退出碼與輸出大小
     *
     * @param ctx 包含沙盒執行所有參數的上下文
     * @return 沙盒執行的合併輸出（stdout + stderr）
     * @throws UnsupportedLanguageException    語言不在白名單
     * @throws SandboxTimeoutException         執行超時
     * @throws SandboxExecutionFailedException 退出碼非 0 或輸出超過大小上限
     */
    @Override
    public String execute(AgentExecutionContext ctx) {
        String language = ctx.sandboxLanguage() != null ? ctx.sandboxLanguage().toLowerCase() : "";
        String sourceCode = ctx.sandboxSourceCode() != null ? ctx.sandboxSourceCode() : "";
        String sandboxType = ctx.resolvedSandboxType(); // LOCAL / DOCKER / E2B
        String dockerImage = (ctx.sandboxDockerImage() != null && !ctx.sandboxDockerImage().isBlank())
                ? ctx.sandboxDockerImage() : dockerDefaultImage;

        log.info("SandboxExecutionProvider.execute taskId={}, language={}, sandboxType={}, timeoutMs={}",
                ctx.taskId(), language, sandboxType, ctx.sandboxTimeoutMs());

        // 1. 語言白名單驗證
        validateLanguage(language);

        // 2. 决定檔案名稱與執行命令
        String filename = resolveFilename(language);
        String command = resolveCommand(language, filename);

        try (var sandbox = createSandbox(sandboxType, dockerImage, ctx.taskId(), filename, sourceCode)) {

            // 3. 建立 ExecSpec（含 timeout）
            ExecSpec.Builder specBuilder = ExecSpec.builder().shellCommand(command);
            if (ctx.hasSandboxTimeout()) {
                specBuilder.timeout(Duration.ofMillis(ctx.sandboxTimeoutMs()));
            }
            ExecSpec spec = specBuilder.build();

            // 4. 執行
            var result = sandbox.exec(spec);
            log.info("Sandbox finished. type={}, exitCode={}, duration={}", sandboxType, result.exitCode(), result.duration());

            // 5. 輸出大小防護
            String merged = result.mergedLog();
            if (merged.length() > maxOutputBytes) {
                throw new SandboxExecutionFailedException(
                        "Sandbox output exceeded max size (" + maxOutputBytes + " bytes)", result.exitCode());
            }

            // 6. 退出碼檢查
            if (result.failed()) {
                throw new SandboxExecutionFailedException(
                        "Sandbox execution failed with exitCode=" + result.exitCode() + ": " + result.stderr(),
                        result.exitCode());
            }

            return merged;

        } catch (UnsupportedLanguageException | SandboxExecutionFailedException e) {
            throw e;
        } catch (Exception e) {
            // 判斷是否為 timeout（zt-exec 拋出 TimeoutException 或包裝在 RuntimeException 內）
            if (isTimeoutException(e)) {
                throw new SandboxTimeoutException(ctx.sandboxTimeoutMs(), e);
            }
            throw new SandboxExecutionFailedException("Sandbox unexpected error: " + e.getMessage());
        }
    }

    // ---- 可覆寫的 sandbox 工廠（方便測試時注入 mock）----

    /**
     * 依 sandboxType 建立對應的 Sandbox 實例。
     *
     * <p>支援類型：</p>
     * <ul>
     *   <li>{@code DOCKER} — 使用 DockerSandbox（Testcontainers 後端）</li>
     *   <li>{@code LOCAL}（預設）— 使用 LocalSandbox（主機直接執行）</li>
     * </ul>
     * <p>E2B 將在加入 agent-sandbox-e2b 依賴後証進：
     * <pre>{@code
     * case "E2B" -> E2BSandbox.builder()
     *     .apiKey(System.getenv("E2B_API_KEY"))
     *     .template(dockerImage != null ? dockerImage : "base")
     *     .withFile(filename, sourceCode)
     *     .build();
     * }</pre></p>
     *
     * @param sandboxType  沙盒類型（LOCAL / DOCKER）
     * @param dockerImage  Docker 映像名稱（DOCKER 模式使用）
     * @param taskId       任務 ID，作為 LOCAL 暫存目錄前綴
     * @param filename     要寫入沙盒工作目錄的檔案名稱
     * @param sourceCode   檔案內容
     * @return 已初始化的 Sandbox 實例
     */
    protected Sandbox createSandbox(
            String sandboxType, String dockerImage, String taskId, String filename, String sourceCode) {
        return switch (sandboxType) {
            case "DOCKER" -> {
                log.info("Creating DockerSandbox with image={}", dockerImage);
                yield DockerSandbox.builder()
                        .image(dockerImage)
                        .withFile(filename, sourceCode)
                        .build();
            }
            case "E2B" -> {
                // template 對應 E2B 的沙盒模板 ID（預設 "base"）；dockerImage 欄位複用為 template
                String template = (dockerImage != null && !dockerImage.isBlank()) ? dockerImage : "base";
                // API Key 優先順序：application.yml > E2B_API_KEY 環境變數
                String resolvedKey = (e2bApiKey != null && !e2bApiKey.isBlank())
                        ? e2bApiKey : System.getenv("E2B_API_KEY");
                log.info("Creating E2BSandbox with template={}", template);
                yield E2BSandbox.builder()
                        .apiKey(resolvedKey)
                        .template(template)
                        .withFile(filename, sourceCode)
                        .build();
            }
            default -> {
                // LOCAL 或不識別類型均 fallback 到 LocalSandbox
                if (!"LOCAL".equals(sandboxType)) {
                    log.warn("Unknown sandboxType '{}', falling back to LOCAL", sandboxType);
                }
                yield LocalSandbox.builder()
                        .tempDirectory("sandbox-" + taskId + "-")
                        .withFile(filename, sourceCode)
                        .build();
            }
        };
    }

    // ---- 私有工具方法 ----

    /**
     * 驗證語言是否在白名單，不符合時拋出 UnsupportedLanguageException。
     */
    private void validateLanguage(String language) {
        if (language.isBlank() || !allowedLanguages.contains(language)) {
            log.warn("Rejected sandbox language: {}", language);
            throw new UnsupportedLanguageException(language);
        }
    }

    /**
     * 依語言決定預置檔案名稱。
     */
    private String resolveFilename(String language) {
        return switch (language) {
            case "python" -> "script.py";
            case "javascript" -> "script.js";
            default -> "script.sh";
        };
    }

    /**
     * 依語言決定執行命令（在 sandbox workDir 內執行）。
     */
    private String resolveCommand(String language, String filename) {
        return switch (language) {
            case "python" -> "python3 " + filename;
            case "javascript" -> "node " + filename;
            default -> "bash " + filename;
        };
    }

    /**
     * 判斷例外是否源自 timeout。
     * <p>涵蓋以下情境：</p>
     * <ul>
     *   <li>{@code org.springaicommunity.sandbox.TimeoutException}（sandbox 自身）</li>
     *   <li>{@code java.util.concurrent.TimeoutException}（zt-exec）</li>
     *   <li>class name 或訊息含 "timeout" / "timed out" 的任何例外</li>
     * </ul>
     */
    private boolean isTimeoutException(Exception e) {
        // 直接類型判斷：sandbox 自己的 TimeoutException
        if (e.getClass().getName().equals("org.springaicommunity.sandbox.TimeoutException")) {
            return true;
        }
        // JDK java.util.concurrent.TimeoutException
        if (e instanceof java.util.concurrent.TimeoutException) {
            return true;
        }
        // class name 兜底（例如 SandboxException wrapping timeout）
        if (e.getClass().getSimpleName().toLowerCase().contains("timeout")) {
            return true;
        }
        // cause chain 檢查
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            if (cause.getClass().getSimpleName().toLowerCase().contains("timeout")) {
                return true;
            }
            cause = cause.getCause();
        }
        // 訊息關鍵字兜底（SandboxException 訊息可能含 "timed out"）
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("timeout") || msg.contains("timed out");
    }
}
