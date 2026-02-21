# 參考資源清單 (Reference Sites Backup)

本目錄 (`docs/references/`) 備份了 Team Work 架構設計初期所參考的核心網站文章，避免未來連結失效。

## 1. OpenClaw (小龍蝦) 相關架構與探討

OpenClaw 作為具備高度本地化、自主運行以及 Webhook 非同步回報機制的多 Agent 系統，其設計理念是 Team Work 的重要參考。

*   **[OpenClaw Architecture Deep Dive (Wisely Chen)](./openclaw_wiselychen.md)**
    *   **簡介**：深入解析 OpenClaw 的架構與工作流。特別提到摒棄 WebSocket 轉而使用 Webhook (Callback) 機制以保證長時間任務的高可用性，以及無狀態 (Stateless) 的 Gateway 設計。
    *   **原始連結**：[連結](https://ai-coding.wiselychen.com/openclaw-architecture-deep-dive-what-claude-code-didnt-tell-you/)
*   **[OpenClaw Tutorial (Meta Intelligence)](./openclaw_tutorial.md)**
    *   **簡介**：OpenClaw 面向開發者與入門者的詳細教學指南與本地部署介紹。
    *   **原始連結**：[連結](https://www.meta-intelligence.tech/insight-openclaw-tutorial)
*   **[OpenClaw Gist 備份](./openclaw_gist.md)**
    *   **簡介**：GitHub Gist 上關於 OpenClaw 的腳本與設定討論。
    *   **原始連結**：[連結](https://gist.github.com/lowkingshih/c1f1a5e93ca314d8b08979b8f53b3060/)
*   **[OpenClaw Taiwan 介紹](./openclaw_accucrazy.md)**
    *   **簡介**：針對台灣社群推廣的 OpenClaw 實際應用案例。
    *   **原始連結**：[連結](https://accucrazy.com/openclaw-taiwan)

## 2. Spring AI Agent Utils 相關技術支持

`spring-ai-community/spring-ai-agent-utils` 提供了在 Java Spring 環境下，模組化載入外部技能 (Skills) 與 MCP Server 的標準作法。

*   **[Spring AI Agent Utils GitHub Repo](./spring_ai_agent_github.md)**
    *   **簡介**：專案的 GitHub 官方主頁，介紹 `SkillsTool`、`TaskTool` 等實作方式以及範例程式碼。
    *   **原始連結**：[連結](https://github.com/spring-ai-community/spring-ai-agent-utils)
*   **[Spring AI 宣佈支持 Agent Skills (Juejin掘金技術文)](./spring_ai_juejin.md)**
*   **[Spring AI Agentic Patterns: Task Subagents](./spring_ai_patterns.md)**
    *   **簡介**：Spring 官方 Blog 文章，講解如何在 Spring AI 框架中實現多層次子 Agent (Subagents)，這是 Team Work Agent Spawning 機制的最主要參考依據。
    *   **原始連結**：[連結](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents)
*   **[DeepWiki: Spring AI Agent Utils](./spring_ai_deepwiki.md)**
    *   **簡介**：DeepWiki 對 Spring AI 工具組件庫的整理。
    *   **原始連結**：[連結](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils)
