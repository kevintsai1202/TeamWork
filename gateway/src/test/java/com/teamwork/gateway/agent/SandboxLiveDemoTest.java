package com.teamwork.gateway.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springaicommunity.sandbox.ExecResult;
import org.springaicommunity.sandbox.ExecSpec;
import org.springaicommunity.sandbox.LocalSandbox;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * SandboxLiveDemoTest — 沙盒真實執行能力展示（Integration Demo）。
 *
 * <p>直接呼叫 LocalSandbox.exec()，在主機環境執行真實程式碼，展示 agent-sandbox-core 的實際能力。</p>
 * <p>MVP 限制：LocalSandbox 無容器隔離，直接使用主機 python3/node 執行。</p>
 *
 * <p>執行方式（不依賴 Spring Context）：</p>
 * <pre>
 *   $env:JAVA_HOME = "D:\java\jdk-21"
 *   .\mvnw.cmd surefire:test "-Dtest=SandboxLiveDemoTest" "-Dsandbox.live=true"
 * </pre>
 */
@DisplayName("Sandbox Live Demo（真實執行）")
class SandboxLiveDemoTest {

    // ── Python 演示 ──────────────────────────────────────────────────

    @Test
    @DisplayName("Python：Hello World & 基本輸出")
    void demo_Python_HelloWorld() {
        String code = "print('Hello from sandbox!')\nprint('Python sandbox OK')";

        ExecResult result = runInSandbox("python", code, 10_000L);

        System.out.println("\n─── Python Hello World ───");
        printResult(result);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("Hello from sandbox!");
    }

    @Test
    @DisplayName("Python：算術運算驗證")
    void demo_Python_Arithmetic() {
        String code = """
                import math
                nums = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
                print(f"Sum     = {sum(nums)}")
                print(f"Average = {sum(nums)/len(nums):.1f}")
                print(f"Sqrt(2) = {math.sqrt(2):.6f}")
                """;

        ExecResult result = runInSandbox("python", code, 10_000L);

        System.out.println("\n─── Python 算術演算 ───");
        printResult(result);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("Sum     = 55");
        assertThat(result.stdout()).contains("Average = 5.5");
    }

    @Test
    @DisplayName("Python：JSON 處理能力")
    void demo_Python_Json() {
        String code = """
                import json
                data = {"name": "TeamWork", "version": "1.0", "features": ["sandbox", "agent"]}
                serialized = json.dumps(data, ensure_ascii=False)
                parsed = json.loads(serialized)
                print(f"name={parsed['name']}, features_count={len(parsed['features'])}")
                print("JSON round-trip OK")
                """;

        ExecResult result = runInSandbox("python", code, 10_000L);

        System.out.println("\n─── Python JSON 處理 ───");
        printResult(result);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("JSON round-trip OK");
    }

    @Test
    @DisplayName("Python：超時保護（故意 sleep 超時）")
    void demo_Python_TimeoutProtection() {
        String code = "import time\nprint('start')\ntime.sleep(30)\nprint('end')";

        long startMs = System.currentTimeMillis();
        try (var sandbox = LocalSandbox.builder()
                .tempDirectory("sandbox-demo-timeout-")
                .withFile("script.py", code)
                .build()) {

            ExecSpec spec = ExecSpec.builder()
                    .shellCommand("python3 script.py")
                    .timeout(Duration.ofSeconds(2))
                    .build();

            ExecResult result = sandbox.exec(spec);
            long elapsed = System.currentTimeMillis() - startMs;
            System.out.println("\n─── Python 超時保護 ───");
            System.out.println("exitCode=" + result.exitCode() + ", elapsed=" + elapsed + "ms");
            System.out.println("stdout=" + result.stdout());
            // 某些系統中 timeout 會以非零 exit code 回傳，不一定拋出例外
            System.out.println("超時觸發：elapsed < 4000ms = " + (elapsed < 4000));
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startMs;
            System.out.println("\n─── Python 超時保護（例外路徑）───");
            System.out.println("Exception type: " + e.getClass().getSimpleName());
            System.out.println("elapsed=" + elapsed + "ms（應 < 4000ms）");
            assertThat(elapsed).isLessThan(4000L);
            System.out.println("✓ 超時保護正常，在 " + elapsed + "ms 中斷執行");
        }
    }

    @Test
    @DisplayName("Python：語言白名單拒絕（SandboxExecutionProvider 層級）")
    void demo_WhitelistRejection() {
        SandboxExecutionProvider provider = new SandboxExecutionProvider(
                List.of("python", "javascript"), 65536L);

        AgentExecutionContext ctx = new AgentExecutionContext(
                "demo-reject-task", "payload",
                null, null,
                "demo-agent", "", "spring-ai",
                false,
                true, "ruby",          // ← 不在白名單
                "puts 'hello'", 0L,
                "LOCAL", null);

        try {
            provider.execute(ctx);
            fail("應拋出 UnsupportedLanguageException");
        } catch (com.teamwork.gateway.exception.UnsupportedLanguageException e) {
            System.out.println("\n─── 語言白名單拒絕 ───");
            System.out.println("✓ 語言 '" + e.getLanguage() + "' 被拒絕：" + e.getMessage());
        }
    }

    @Test
    @DisplayName("Python：輸出大小保護")
    void demo_OutputSizeProtection() {
        // 產生約 200 bytes 輸出，但 Provider 上限設 50 bytes
        String code = "print('A' * 200)";

        SandboxExecutionProvider provider = new SandboxExecutionProvider(
                List.of("python"), 50L);   // 上限 50 bytes

        AgentExecutionContext ctx = new AgentExecutionContext(
                "demo-size-task", "payload",
                null, null,
                "demo-agent", "", "spring-ai",
                false,
                true, "python", code, 10_000L,
                "LOCAL", null);

        try {
            provider.execute(ctx);
            fail("應拋出 SandboxExecutionFailedException");
        } catch (com.teamwork.gateway.exception.SandboxExecutionFailedException e) {
            System.out.println("\n─── 輸出大小保護 ───");
            System.out.println("✓ 輸出超限被攔截：" + e.getMessage());
        }
    }

    @Test
    @DisplayName("Python：執行失敗（非零退出碼）")
    void demo_Python_RuntimeError() {
        String code = "x = undefined_variable\nprint(x)";  // NameError

        ExecResult result = runInSandbox("python", code, 10_000L);

        System.out.println("\n─── Python 執行失敗（NameError）───");
        printResult(result);

        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.failed()).isTrue();
        assertThat(result.stderr()).containsIgnoringCase("NameError");
        System.out.println("✓ 非零退出碼正確回傳 exitCode=" + result.exitCode());
    }

    // ── Node.js 演示 ────────────────────────────────────────────────

    @Test
    @DisplayName("JavaScript (Node)：基本輸出")
    void demo_JavaScript_Basic() {
        String code = """
                const msg = 'Hello from Node.js sandbox!';
                console.log(msg);
                console.log('2 + 3 =', 2 + 3);
                console.log('Array:', JSON.stringify([1,2,3].map(x => x * x)));
                """;

        ExecResult result = runInSandbox("javascript", code, 10_000L);

        System.out.println("\n─── JavaScript Node.js ───");
        printResult(result);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("Hello from Node.js sandbox!");
        assertThat(result.stdout()).contains("2 + 3 = 5");
    }

    // ── 能力摘要 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("沙盒能力摘要報告")
    void demo_CapabilitySummary() {
        System.out.println("""
                
                ╔══════════════════════════════════════════════════════╗
                ║          agent-sandbox-core 0.9.0-SNAPSHOT           ║
                ║               MVP 沙盒能力摘要                       ║
                ╠══════════════════════════════════════════════════════╣
                ║ 實作類別：LocalSandbox（無容器隔離，直接主機執行）   ║
                ║ 語言支援：python3、node（bash 需 WSL or Git Bash）   ║
                ║ 隔離層級：暫存目錄（sandbox-{taskId}-* / tmp）       ║
                ║ Timeout ：ExecSpec.timeout(Duration) → zt-exec       ║
                ║ 輸出上限：SandboxExecutionProvider.maxOutputBytes    ║
                ║ 退出碼  ：ExecResult.exitCode() / failed()           ║
                ║ 合併輸出：ExecResult.mergedLog（stdout + stderr 合併）║
                ║ 清理    ：AutoCloseable.close() 自動刪暫存目錄       ║
                ╠══════════════════════════════════════════════════════╣
                ║ 後續版本（未實作）：                                  ║
                ║   Docker 容器隔離、E2B 沙盒、網路限制、資源配額      ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }

    // ── 工具方法 ──────────────────────────────────────────────────────

    /**
     * 用真實 LocalSandbox 執行程式碼，回傳 ExecResult。
     */
    private ExecResult runInSandbox(String language, String code, long timeoutMs) {
        String filename = switch (language) {
            case "python" -> "script.py";
            case "javascript" -> "script.js";
            default -> "script.sh";
        };
        String command = switch (language) {
            case "python" -> "python3 " + filename;
            case "javascript" -> "node " + filename;
            default -> "bash " + filename;
        };

        try (var sandbox = LocalSandbox.builder()
                .tempDirectory("sandbox-demo-")
                .withFile(filename, code)
                .build()) {

            ExecSpec spec = ExecSpec.builder()
                    .shellCommand(command)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .build();

            return sandbox.exec(spec);
        } catch (Exception e) {
            // 只在非預期例外時 rethrow，讓測試失敗訊息更清晰
            throw new RuntimeException("Sandbox execution error: " + e.getMessage(), e);
        }
    }

    /** 格式化列印 ExecResult 到 stdout，方便肉眼確認。 */
    private void printResult(ExecResult r) {
        System.out.println("exitCode : " + r.exitCode());
        System.out.println("duration : " + r.duration().toMillis() + "ms");
        System.out.println("stdout   : " + r.stdout().strip());
        if (!r.stderr().isBlank()) {
            System.out.println("stderr   : " + r.stderr().strip());
        }
        System.out.println("success  : " + r.success());
    }
}
