Index your code with Devin

[DeepWiki](https://deepwiki.com/)

[DeepWiki](https://deepwiki.com/)

[spring-ai-community/spring-ai-agent-utils](https://github.com/spring-ai-community/spring-ai-agent-utils "Open repository")

Index your code with

Devin
Edit WikiShare

Last indexed: 15 January 2026 ( [510f14](https://github.com/spring-ai-community/spring-ai-agent-utils/commits/510f1493))

- [Overview](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/1-overview)
- [Getting Started](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/2-getting-started)
- [Installation & Maven Setup](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/2.1-installation-and-maven-setup)
- [Configuration Basics](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/2.2-configuration-basics)
- [Your First Agent](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/2.3-your-first-agent)
- [Core Concepts](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/3-core-concepts)
- [ChatClient Configuration](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/3.1-chatclient-configuration)
- [Agent Environment & System Prompts](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/3.2-agent-environment-and-system-prompts)
- [Tools vs Tool Callbacks](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/3.3-tools-vs-tool-callbacks)
- [Advisors & Chat Memory](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/3.4-advisors-and-chat-memory)
- [Core Tools Reference](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/4-core-tools-reference)
- [File System Tools](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/4.1-file-system-tools)
- [Shell Tools](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/4.2-shell-tools)
- [Web Access Tools](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/4.3-web-access-tools)
- [Skills Tool](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/4.4-skills-tool)
- [User Interaction Tools](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/4.5-user-interaction-tools)
- [Todo Write Tool](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/4.6-todo-write-tool)
- [Multi-Agent Architecture](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/5-multi-agent-architecture)
- [Task Tools & Sub-Agent Orchestration](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/5.1-task-tools-and-sub-agent-orchestration)
- [Sub-Agent Configuration](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/5.2-sub-agent-configuration)
- [Built-in Sub-Agents](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/5.3-built-in-sub-agents)
- [Background Task Execution](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/5.4-background-task-execution)
- [Demo Applications](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/6-demo-applications)
- [Skills Demo](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/6.1-skills-demo)
- [Code Agent Demo](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/6.2-code-agent-demo)
- [Subagent Demo](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/6.3-subagent-demo)
- [Ask User Question Demo](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/6.4-ask-user-question-demo)
- [Advanced Topics](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/7-advanced-topics)
- [Custom Skill Development](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/7.1-custom-skill-development)
- [Custom Tool Development](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/7.2-custom-tool-development)
- [Multi-Model Support](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/7.3-multi-model-support)
- [Testing & Validation](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/7.4-testing-and-validation)
- [API Reference](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/8-api-reference)
- [Builder APIs](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/8.1-builder-apis)
- [Configuration Properties](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/8.2-configuration-properties)
- [Maven Coordinates & Dependencies](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/8.3-maven-coordinates-and-dependencies)

Menu

# Overview

Relevant source files

- [README.md](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/README.md)
- [spring-ai-agent-utils/README.md](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/spring-ai-agent-utils/README.md)

This document introduces the **spring-ai-agent-utils** library, a Spring AI toolkit that implements Claude Code-inspired agent capabilities for building autonomous AI assistants. It covers the library's purpose, architectural positioning within the Spring AI ecosystem, core components, and project structure.

For detailed tool usage and configuration, see [Core Tools Reference](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/4-core-tools-reference). For multi-agent orchestration patterns, see [Multi-Agent Architecture](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/5-multi-agent-architecture). For initial setup instructions, see [Getting Started](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/2-getting-started).

* * *

## Purpose and Scope

**spring-ai-agent-utils** is a Java library that extends [Spring AI](https://docs.spring.io/spring-ai/reference/) with tools and patterns reverse-engineered from [Claude Code](https://code.claude.com/docs/en/overview). It provides:

- **File and Shell Operations**: Read, write, edit files; execute shell commands with background process management
- **Code Search and Navigation**: Regex-based grep, glob pattern matching for codebase exploration
- **Web Access**: AI-powered web content summarization and search via Brave API
- **Knowledge Modules**: Reusable skill definitions in Markdown for domain-specific workflows
- **Multi-Agent Orchestration**: Hierarchical sub-agent system with specialized task delegation
- **User Interaction**: Question-answer tools for clarifying requirements during execution
- **Task Management**: Structured todo list tracking for complex operations

The library targets Java developers building AI coding assistants, autonomous research agents, or any system requiring agentic behavior with external tool access.

**Sources**: [README.md1-17](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/README.md#L1-L17) [spring-ai-agent-utils/README.md1-37](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/spring-ai-agent-utils/README.md#L1-L37)

* * *

## Architectural Positioning

The library acts as a bridge between Spring AI's generic LLM capabilities and practical software engineering tasks. It integrates with Spring AI's `ChatClient` API using the builder pattern to register tools, tool callbacks, and advisors.

### System Integration Diagram

```
LLM Providers

Application Layer

spring-ai-agent-utils Library

Spring AI Framework

Context Management

User Interaction

Agent Intelligence

Tool Implementations

.defaultTools()

.defaultTools()

.defaultTools()

.defaultTools()

.defaultTools()

.defaultTools()

.defaultToolCallbacks()

.defaultToolCallbacks()

.defaultTools()

.defaultTools()

system prompt params

Spring AI Core
(org.springframework.ai)

ChatClient API
(ChatClient.Builder)

spring-ai-agent-utils
(org.springaicommunity.agent.utils)

FileSystemTools

ShellTools

GrepTool

GlobTool

SmartWebFetchTool

BraveWebSearchTool

SkillsTool
(ToolCallbackProvider)

TaskToolCallbackProvider
(ToolCallbackProvider)

AskUserQuestionTool

TodoWriteTool

AgentEnvironment
(static utilities)

code-agent-demo

skills-demo

subagent-demo

ask-user-question-demo

Anthropic Claude
(AnthropicChatModel)

OpenAI GPT
(OpenAiChatModel)

Google Gemini
(GoogleGenAIChatModel)
```

The `ChatClient.Builder` provides three extension points:

1. **`.defaultTools()`**: Registers tool objects with `@Tool`-annotated methods
2. **`.defaultToolCallbacks()`**: Registers `ToolCallbackProvider` implementations for dynamic tool generation
3. **`.defaultAdvisors()`**: Registers request/response interceptors for cross-cutting concerns

**Sources**: [README.md68-148](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/README.md#L68-L148) [spring-ai-agent-utils/README.md55-119](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/spring-ai-agent-utils/README.md#L55-L119)

* * *

## Core Component Taxonomy

The library organizes tools into five functional categories:

| Category | Components | Purpose |
| --- | --- | --- |
| **Foundation Tools** | `FileSystemTools`, `ShellTools`, `GrepTool`, `GlobTool` | Atomic file/shell/search operations that form the base layer |
| **Web Access** | `SmartWebFetchTool`, `BraveWebSearchTool` | External information retrieval with AI-powered summarization |
| **Knowledge & Orchestration** | `SkillsTool`, `TaskToolCallbackProvider` | Higher-level abstractions that compose foundation tools |
| **User Interaction** | `AskUserQuestionTool`, `TodoWriteTool` | Bidirectional communication with human operators |
| **Runtime Context** | `AgentEnvironment` | System-level metadata injection for prompt awareness |

### Component Dependency Map

```
Orchestration Layer

Knowledge Layer

Foundation Layer

reads

pairs with

pairs with

reads

creates sub-agents with

creates sub-agents with

creates sub-agents with

creates sub-agents with

stores tasks in

searches files from

Context Layer

injects into

AgentEnvironment
Static methods:
info(), gitStatus()

System Prompt Template
MAIN_AGENT_SYSTEM_PROMPT_V2.md

User Interaction Layer

invokes

AskUserQuestionTool
@Tool method:
askUserQuestion()

QuestionHandler
(callback interface)

TodoWriteTool
@Tool method:
todoWrite()

Web Layer

requires

requires

SmartWebFetchTool
@Tool method:
webFetch()

Nested ChatClient
(for AI summarization)

BraveWebSearchTool
@Tool method:
webSearch()

BRAVE_API_KEY
(env variable)

FileSystemTools
@Tool methods:
read(), write(), edit()

ShellTools
@Tool methods:
bash(), bashOutput(), killShell()

GrepTool
@Tool method:
grep()

GlobTool
@Tool method:
glob()

SkillsTool
(ToolCallbackProvider)
Dynamic @Tool generation

.claude/skills/**/*.md
(Markdown skill definitions)

TaskToolCallbackProvider
(ToolCallbackProvider)
Generates: TaskTool, TaskOutputTool

.claude/agents/**/*.md
(Sub-agent definitions)

TaskRepository
(background task storage)
```

**Key Architectural Patterns**:

- **Composition over Inheritance**: Higher-level tools (`SkillsTool`, `TaskToolCallbackProvider`) compose foundation tools rather than extending them
- **Dynamic Tool Generation**: `ToolCallbackProvider` implementations scan directories at runtime to generate tools from Markdown definitions
- **Isolated Context Windows**: `TaskToolCallbackProvider` creates sub-agents with dedicated `ChatClient` instances to prevent conversation pollution
- **Callback-Based Integration**: `AskUserQuestionTool` uses handler functions to decouple tool logic from UI implementation

**Sources**: [README.md33-60](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/README.md#L33-L60) [spring-ai-agent-utils/README.md9-36](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/spring-ai-agent-utils/README.md#L9-L36)

* * *

## Project Structure

The repository follows a multi-module Maven structure with clear separation between library code and demonstration applications.

### Maven Module Hierarchy

```
Key Dependencies

Examples Module

Core Library

module

module

depends on

depends on

depends on

depends on

provided

compile

parent

spring-ai-agent-utils-parent
(pom.xml)
groupId: org.springaicommunity
version: 0.4.0-SNAPSHOT

spring-ai-agent-utils
(spring-ai-agent-utils/pom.xml)
artifactId: spring-ai-agent-utils

src/main/java/
org.springaicommunity.agent.utils/

src/test/java/
org.springaicommunity.agent.utils/

docs/
(Tool documentation)

FileSystemTools.java

ShellTools.java

GrepTool.java

GlobTool.java

SmartWebFetchTool.java

BraveWebSearchTool.java

SkillsTool.java

TaskToolCallbackProvider.java

AskUserQuestionTool.java

TodoWriteTool.java

AgentEnvironment.java

examples
(examples/pom.xml)
Aggregator POM

code-agent-demo
(examples/code-agent-demo/)

skills-demo
(examples/skills-demo/)

subagent-demo
(examples/subagent-demo/)

ask-user-question-demo
(examples/ask-user-question-demo/)

Spring AI BOM
2.0.0-M1
(provided scope)

Spring Boot
4.0.x
(parent)

flexmark-html2md-converter
(compile scope)
```

### Key Configuration Files

| File Path | Purpose |
| --- | --- |
| `pom.xml` | Parent POM defining Spring AI BOM (2.0.0-M1), Spring Boot version, plugin management |
| `spring-ai-agent-utils/pom.xml` | Core library module with `provided` scope for Spring AI dependencies |
| `examples/pom.xml` | Aggregator POM for all demo applications |
| `spring-ai-agent-utils/src/main/resources/prompt/MAIN_AGENT_SYSTEM_PROMPT_V2.md` | Default system prompt template with `AgentEnvironment` parameter placeholders |
| `.claude/agents/` | Directory convention for custom sub-agent definitions (used by demo apps) |
| `.claude/skills/` | Directory convention for skill definitions (used by demo apps) |

**Dependency Strategy**:

- Core library uses `provided` scope for Spring AI to avoid version conflicts in consuming applications
- `flexmark-html2md-converter` is bundled with `compile` scope for `SmartWebFetchTool` HTML-to-Markdown conversion
- Demo applications declare explicit LLM provider dependencies (e.g., `spring-ai-google-genai-spring-boot-starter`)

**Sources**: [README.md18-31](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/README.md#L18-L31) [README.md160-176](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/README.md#L160-L176)

* * *

## Integration Patterns

The library demonstrates three primary integration patterns in the Quick Start example:

### ChatClient Configuration Phases

```
"SkillsTool""TaskToolCallbackProvider""AgentEnvironment""ChatClient.Builder""Application""SkillsTool""TaskToolCallbackProvider""AgentEnvironment""ChatClient.Builder""Application"Phase 1: System PromptInjects ENVIRONMENT_INFO,GIT_STATUS, AGENT_MODEL,KNOWLEDGE_CUTOFFPhase 2: Tool CallbacksRegisters TaskTool,TaskOutputToolScans .claude/skills/,generates @Tool methodsPhase 3: Direct ToolsRegisters @Tool-annotatedmethods from each instancePhase 4: AdvisorsEnables tool invocation,500-message historyAgentEnvironment.info()OS, directory, date, timezoneAgentEnvironment.gitStatus()branch, uncommitted changes.defaultSystem(promptTemplate)TaskToolCallbackProvider.builder()TaskToolCallbackProvider instance.defaultToolCallbacks(taskTools)SkillsTool.builder()SkillsTool instance.defaultToolCallbacks(skillsTool).defaultTools(ShellTools,FileSystemTools,GrepTool, GlobTool,SmartWebFetchTool,BraveWebSearchTool,TodoWriteTool,AskUserQuestionTool).defaultAdvisors(ToolCallAdvisor,MessageChatMemoryAdvisor).build()ChatClient instance
```

### Tool Composition Example

The following example from [README.md86-147](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/README.md#L86-L147) demonstrates how tools compose:

```
ChatClient chatClient = chatClientBuilder
    // 1. System Prompt with Runtime Context
    .defaultSystem(p -> p.text(agentSystemPrompt)
        .param(AgentEnvironment.ENVIRONMENT_INFO_KEY, AgentEnvironment.info())
        .param(AgentEnvironment.GIT_STATUS_KEY, AgentEnvironment.gitStatus())
        .param(AgentEnvironment.AGENT_MODEL_KEY, "claude-sonnet-4-5-20250929")
        .param(AgentEnvironment.AGENT_MODEL_KNOWLEDGE_CUTOFF_KEY, "2025-01-01"))

    // 2. Dynamic Tools from Directories
    .defaultToolCallbacks(TaskToolCallbackProvider.builder()
        .agentDirectories(".claude/agents")
        .skillsDirectories(".claude/skills")
        .chatClientBuilder(chatClientBuilder.clone())
        .build())

    .defaultToolCallbacks(SkillsTool.builder()
        .addSkillsDirectory(".claude/skills")
        .build())

    // 3. Static Tool Instances
    .defaultTools(
        ShellTools.builder().build(),
        FileSystemTools.builder().build(),
        GrepTool.builder().build(),
        GlobTool.builder().build(),
        SmartWebFetchTool.builder(chatClient).build(),
        BraveWebSearchTool.builder(braveApiKey).build(),
        TodoWriteTool.builder().build(),
        AskUserQuestionTool.builder()
            .questionHandler(questions -> handleUserQuestions(questions))
            .build())

    // 4. Advisors for Tool Invocation and Memory
    .defaultAdvisors(
        ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
        MessageChatMemoryAdvisor.builder(
            MessageWindowChatMemory.builder().maxMessages(500).build()).build())

    .build();
```

**Configuration Order Significance**:

1. **System Prompt First**: Establishes agent identity and capabilities before tools are registered
2. **Tool Callbacks Second**: `TaskToolCallbackProvider` and `SkillsTool` scan directories to dynamically register tools
3. **Direct Tools Third**: Static tool instances with fixed configuration
4. **Advisors Last**: Wrap tool invocation logic and manage conversation history after all tools are registered

**Sources**: [README.md68-147](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/README.md#L68-L147) [spring-ai-agent-utils/README.md55-119](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/spring-ai-agent-utils/README.md#L55-L119)

* * *

## Demo Applications

Four demonstration applications showcase progressive complexity patterns:

| Application | Focus | Key Features | Use Case |
| --- | --- | --- | --- |
| **ask-user-question-demo** | User interaction | `AskUserQuestionTool`, basic memory | Minimal agent with clarifying questions |
| **skills-demo** | Knowledge modules | `SkillsTool`, custom skill development | Agent with domain-specific workflows |
| **code-agent-demo** | Full AI assistant | All tools, MCP integration, `TodoWriteTool` | Complete coding assistant with task management |
| **subagent-demo** | Multi-agent orchestration | `TaskToolCallbackProvider`, custom sub-agents | Hierarchical task delegation |

All demos use Google Gemini as the default LLM provider, but support Claude and OpenAI through configuration. Each demo includes an interactive CLI for testing agent capabilities.

For detailed walkthrough of each demo, see [Demo Applications](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/6-demo-applications).

**Sources**: [README.md178-187](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/README.md#L178-L187) [spring-ai-agent-utils/README.md122-130](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/510f1493/spring-ai-agent-utils/README.md#L122-L130)

* * *

## Next Steps

- **For installation**: See [Installation & Maven Setup](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/2.1-installation-and-maven-setup)
- **For tool configuration**: See [ChatClient Configuration](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/3.1-chatclient-configuration)
- **For tool reference**: See [Core Tools Reference](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/4-core-tools-reference)
- **For multi-agent patterns**: See [Multi-Agent Architecture](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/5-multi-agent-architecture)
- **For custom skill development**: See [Custom Skill Development](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils/7.1-custom-skill-development)

Dismiss

Refresh this wiki

Enter email to refresh

### On this page

- [Overview](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#overview)
- [Purpose and Scope](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#purpose-and-scope)
- [Architectural Positioning](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#architectural-positioning)
- [System Integration Diagram](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#system-integration-diagram)
- [Core Component Taxonomy](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#core-component-taxonomy)
- [Component Dependency Map](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#component-dependency-map)
- [Project Structure](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#project-structure)
- [Maven Module Hierarchy](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#maven-module-hierarchy)
- [Key Configuration Files](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#key-configuration-files)
- [Integration Patterns](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#integration-patterns)
- [ChatClient Configuration Phases](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#chatclient-configuration-phases)
- [Tool Composition Example](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#tool-composition-example)
- [Demo Applications](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#demo-applications)
- [Next Steps](https://deepwiki.com/spring-ai-community/spring-ai-agent-utils#next-steps)

Ask Devin about spring-ai-community/spring-ai-agent-utils

Fast
