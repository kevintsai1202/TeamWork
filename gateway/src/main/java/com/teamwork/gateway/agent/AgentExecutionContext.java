package com.teamwork.gateway.agent;

import com.teamwork.gateway.entity.AiModel;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Agent 執行上下文，統一承載不同 Agent Provider 所需的輸入資料。
 * <p>包含基本任務資料，以及 T14 沙盒執行所需的額外資訊。</p>
 */
public record AgentExecutionContext(
        /** 任務唯一識別碼 */
        String taskId,
        /** 使用者輸入的原始內容 */
        String inputPayload,
        /** 指定使用的 AI 模型設定 */
        AiModel aiModel,
        /** 已初始化的 ChatModel 實例 */
        ChatModel chatModel,
        /** 由路由決策選定的 Sub-Agent 名稱 */
        String selectedSubAgentName,
        /** Sub-Agent 配置檔的相對路徑 */
        String selectedSubAgentReferencePath,
        /** 負責該 Sub-Agent 的 Provider 名稱 */
        String selectedSubAgentOwnerProvider,
        /** 是否畢路收路由（找不到最佳匹配時用 fallback） */
        boolean routeFallbackUsed,
        // ---- T14 沙盒執行欄位 ----
        /** 是否啟用沙盒模式（來自 AgentProfile.sandboxEnabled） */
        boolean sandboxEnabled,
        /** 沙盒語言，對應白名單檢查（如 python / javascript / bash） */
        String sandboxLanguage,
        /** 要在沙盒中執行的程式碼或指令 */
        String sandboxSourceCode,
        /** 沙盒執行超時限制（毫秒），0 表示無限制 */
        long sandboxTimeoutMs,
        /**
         * 沙盒實作類型：LOCAL（預設）/ DOCKER / E2B（未來）。
         * null 等同於 LOCAL。
         */
        String sandboxType,
        /**
         * Docker/E2B 鏡像或模板名稱；null 時各實作使用自身預設值。
         * sandboxType=DOCKER → Docker image；sandboxType=E2B → E2B template。
         */
        String sandboxDockerImage) {

    /** 返回沙盒超時是否啟用 */
    public boolean hasSandboxTimeout() {
        return sandboxTimeoutMs > 0;
    }

    /** 回傳正規化後的沙盒類型（null / 空白 → "LOCAL"，其餘大寫化） */
    public String resolvedSandboxType() {
        return (sandboxType != null && !sandboxType.isBlank())
                ? sandboxType.toUpperCase()
                : "LOCAL";
    }
}

