[Skip to main content](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents#main)

[Home](https://spring.io/ "Home")

Why Spring

- [Overview](https://spring.io/why-spring)
- Trending

- [Generative AI](https://spring.io/ai)
- [Cloud](https://spring.io/cloud)
- Architecture Patterns

- [Microservices](https://spring.io/microservices)
- [Reactive](https://spring.io/reactive)
- [Event Driven](https://spring.io/event-driven)
- Application Types

- [Web Applications](https://spring.io/web-applications)
- [Serverless](https://spring.io/serverless)
- [Batch](https://spring.io/batch)

Learn

- Getting Started

- [Quickstart](https://spring.io/quickstart)
- [Guides](https://spring.io/guides)
- Academy

- [Courses](https://spring.academy/courses)
- [Get Certified](https://spring.academy/learning-path)

Projects

- [Overview](https://spring.io/projects)
- Projects

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Framework](https://spring.io/projects/spring-framework)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Spring AI](https://spring.io/projects/spring-ai)
- [Spring Data](https://spring.io/projects/spring-data)
- [Spring Integration](https://spring.io/projects/spring-integration)
- [Spring Batch](https://spring.io/projects/spring-batch)
- [Spring Security](https://spring.io/projects/spring-security)
- Foundational Projects

- [Micrometer](https://micrometer.io/)
- [Reactor](https://projectreactor.io/)
- Development Tools

- [Spring Tools](https://spring.io/tools)
- [Spring Initializr](https://start.spring.io/)

Resources

- [Blog](https://spring.io/blog)
- [Release Calendar](https://spring.io/projects#release-calendar)
- [Version Mappings](https://spring.io/projects/generations)
- [Security Advisories](https://spring.io/security)
- GitHub Orgs

- [Spring Projects](https://github.com/spring-projects)
- [Spring Cloud](https://github.com/spring-cloud)

Community

- [Overview](https://spring.io/community)
- [Events](https://spring.io/events)
- [Authors](https://spring.io/authors)

Enterprise

- [Overview](https://enterprise.spring.io/)
- [Long-term Support](https://enterprise.spring.io/lts-releases)
- [Automated Upgrades](https://enterprise.spring.io/spring-application-advisor)
- [Governance and Compliance](https://enterprise.spring.io/enterprise-extensions)
- [Modern App Development](https://enterprise.spring.io/enterprise-components)

light

[Logo](https://spring.io/ "Logo")

# [Spring blog](https://spring.io/blog)

[All Posts](https://spring.io/blog)

[Engineering](https://spring.io/blog/category/engineering)

[Releases](https://spring.io/blog/category/releases)

[News and Events](https://spring.io/blog/category/news)

RSS feeds

[All Posts](https://spring.io/blog.atom)

[Engineering](https://spring.io/blog/category/engineering.atom)

[Releases](https://spring.io/blog/category/releases.atom)

[News and Events](https://spring.io/blog/category/news.atom)

# Spring AI Agentic Patterns (Part 4): Subagent Orchestration

[Engineering](https://spring.io/blog/category/engineering) \| [Christian Tzolov](https://spring.io/team/tzolov) \| January 27, 2026 \| [1 Comment](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents#disqus_thread)

![](https://raw.githubusercontent.com/spring-io/spring-io-static/refs/heads/main/blog/tzolov/20260127/subagents.png)

Instead of one generalist agent doing everything, delegate to specialized agents. This keeps context windows focused?reventing the clutter that degrades performance.

[Task tool](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/TaskTools.md), part of the [spring-ai-agent-utils](https://github.com/spring-ai-community/spring-ai-agent-utils) toolkit, is a **portable, model-agnostic** Spring AI implementation inspired by [Claude Code's subagents](https://platform.claude.com/docs/en/agent-sdk/subagents). It enables hierarchical agent architectures where specialized subagents handle focused tasks in **dedicated context windows**, returning only essential results to the parent. Beyond Claude's markdown-based format, the architecture is extensible?upporting [A2A](https://google.github.io/A2A/) and other agentic protocols for heterogeneous agent orchestration (more information will be provided in a follow up post).

**This is Part 4 of our Spring AI Agentic Patterns series.** We've covered [Agent Skills](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills), [AskUserQuestionTool](https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool), and [TodoWriteTool](https://spring.io/blog/2026/01/20/spring-ai-agentic-patterns-3-todowrite/). Now we explore hierarchical subagents.

**Ready to dive in?** Skip to [Getting Started](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents#getting-started).

## [how it works permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#how-it-works) How It Works

The main agent delegates tasks to specialized subagents through the Task tool, with each subagent operating in its own isolated context window. The subagent architecture consists of three key components:

**1\. Main Agent (Orchestrator)**
The primary agent that interacts with users. Its LLM has access to the `Task` tool and knows about available subagents through the **Agent Registry**? catalog of subagent names and descriptions populated at startup. The main agent automatically decides when to delegate based on each subagent's `description` field.

**2\. Agent Configuration Files**
Subagents are defined as Markdown files (e.g., `agent-x.md`, `agent-y.md`) in an `agents/` folder. Each file specifies the subagent's name, description, allowed tools, preferred model, and system prompt. These configurations populate both the Agent Registry and Task tool at startup.

**3\. Subagents**
Separate agent instances that execute in isolated context windows. Each subagent can use a **different LLM** (LLM-X, LLM-Y, LLM-Z) with its own system prompt, tools, and skills?nabling multi-model routing based on task complexity.

The diagram below illustrates the execution flow:

![](https://raw.githubusercontent.com/spring-io/spring-io-static/refs/heads/main/blog/tzolov/20260127/sub-agents-architecture.png)

1. **Loading:** At startup, the Task tool loads the configured subagent references, resolves their names and descriptions, and populates the agent registry.
2. **User** sends a complex question to the main agent
3. **Main agent's LLM** evaluates the request and checks available subagents in the registry
4. **LLM decides** to delegate by invoking the `Task` tool with the subagent name and task description
5. **Task tool** spawns the appropriate subagent based on the agent configuration
6. **Subagent** works autonomously in its dedicated context window
7. **Results** flow back to the main agent (only essential findings, not intermediate steps)
8. **Main agent** synthesizes and returns the final answer to the user

Each subagent operates with:

- **Dedicated context window** \- Isolated from the main conversation, preventing clutter
- **Custom system prompt** \- Tailored expertise for specific domains
- **Configurable tool access** \- Restricted to only necessary capabilities
- **Multi-model routing** \- Route simple tasks to cheaper models, complex analysis to capable ones
- **Parallel execution** \- Launch multiple subagents concurrently
- **Background tasks** \- Long-running operations execute asynchronously

### [built in subagents permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#built-in-subagents) Built-in Subagents

Spring AI Agent Utils provides four built-in subagents, automatically registered when `TaskTool` is configured:

| Subagent | Purpose | Tools |
| :-- | :-- | :-- |
| **Explore** | Fast, read-only codebase exploration?ind files, search code, analyze contents | Read, Grep, Glob |
| **General-Purpose** | Multi-step research and execution with full read/write access | All tools |
| **Plan** | Software architect for designing implementation strategies and identifying trade-offs | Read-only + search |
| **Bash** | Command execution specialist for git operations, builds, and terminal tasks | Bash only |

See the [reference documentation](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/TaskTools.md#built-in-sub-agents) for detailed capabilities. Multiple subagents can run concurrently?or example, running `style-checker`, `security-scanner`, and `test-coverage` simultaneously during code review.

## [getting started permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#getting-started) Getting Started

### [1 add the dependency permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#1-add-the-dependency) 1\. Add the Dependency

```xml
Copy<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-agent-utils</artifactId>
    <version>0.4.2</version>
</dependency>
```

### [2 configure your agent permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#2-configure-your-agent) 2\. Configure Your Agent

```java
Copyimport org.springaicommunity.agent.tools.task.TaskToolCallbackProvider;

@Configuration
public class AgentConfig {

    @Bean
    CommandLineRunner demo(ChatClient.Builder chatClientBuilder) {
        return args -> {
            // Configure Task tools
            var taskTools = TaskToolCallbackProvider.builder()
                .chatClientBuilder("default", chatClientBuilder)
                .subagentReferences(
                    ClaudeSubagentReferences.fromRootDirectory("src/main/resources/agents"))
                .build();

            // Build main chat client with Task tools
            ChatClient chatClient = chatClientBuilder
                .defaultToolCallbacks(taskTools)
                .build();

            // Use naturally - agent will delegate to subagents
            String response = chatClient
                .prompt("Explore the authentication module and explain how it works")
                .call()
                .content();
        };
    }
}
```

The main agent automatically recognizes when to delegate to subagents based on their `description` fields.

### [3 multi model routing optional permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#3-multi-model-routing-optional) 3\. Multi-Model Routing (Optional)

Route subagents to different models based on task complexity:

```java
Copyvar taskTools = TaskToolCallbackProvider.builder()
    .chatClientBuilder("default", sonnetBuilder)   // Default model
    .chatClientBuilder("haiku", haikuBuilder)      // Fast, cheap
    .chatClientBuilder("opus", opusBuilder)        // Complex analysis
    .build();
```

Subagents specify their preferred model in their definition, and the Task tool routes accordingly.

## [creating custom subagents permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#creating-custom-subagents) Creating Custom Subagents

Custom subagents are Markdown files with YAML frontmatter, typically stored in `.claude/agents/`:

```
Copyproject-root/
??? .claude/
??  ??? agents/
??      ??? code-reviewer.md
??      ??? test-runner.md
```

#### [subagent file format permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#subagent-file-format) Subagent File Format

```markdown
Copy---
name: code-reviewer
description: Expert code reviewer. Use proactively after writing code.
tools: Read, Grep, Glob
disallowedTools: Edit, Write
model: sonnet
---

You are a senior code reviewer with expertise in software quality.

**When Invoked:**
1. Run `git diff` to see recent changes
2. Focus analysis on modified files
3. Check surrounding code context

**Review Checklist:**
- Code clarity and readability
- Proper naming conventions
- Error handling
- Security vulnerabilities

**Output:** Clear, actionable feedback with file references.
```

#### [configuration fields permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#configuration-fields) Configuration Fields

| Field | Required | Description |
| --- | --- | --- |
| `name` | Yes | Unique identifier (lowercase with hyphens) |
| `description` | Yes | Natural language description of when to use this subagent |
| `tools` | No | Allowed tool names (inherits all if omitted) |
| `disallowedTools` | No | Tools to explicitly deny |
| `model` | No | Model preference: `haiku`, `sonnet`, `opus` |

See the [reference documentation](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/TaskTools.md#creating-custom-sub-agents) for additional fields like `skills` and `permissionMode`.

> **Important:** Subagents cannot spawn their own subagents. Don't include `Task` in a subagent's `tools` list.

#### [loading custom subagents permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#loading-custom-subagents) Loading Custom Subagents

```java
Copyimport org.springaicommunity.agent.tools.task.subagent.claude.ClaudeSubagentReferences;

var taskTools = TaskToolCallbackProvider.builder()
    .chatClientBuilder("default", chatClientBuilder)
    .subagentReferences(
        ClaudeSubagentReferences.fromRootDirectory("src/main/resources/agents")
    )
    .build();
```

## [background execution permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#background-execution) Background Execution

Long-running subagents can execute asynchronously.
The main agent continues working while background subagents execute. Use `TaskOutputTool` to retrieve results when needed. For persistent task storage across instances, see the [TaskRepository documentation](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/TaskTools.md#background-task-management).

## [conclusion permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#conclusion) Conclusion

The Task tool brings hierarchical subagent architectures to Spring AI, enabling context isolation, specialized instructions, and efficient multi-model routing. By delegating complex tasks to focused subagents, your main agent stays lean and responsive.

**Next up:** In [Part 5](https://spring.io/blog/2026/01/29/spring-ai-agentic-patterns-a2a-integration), we explore **A2A Integration**?uilding interoperable agents with the Agent2Agent protocol. In a follow-up post, we'll cover the **Subagent Extension Framework**? protocol-agnostic abstraction for integrating remote agents via A2A, MCP, or custom protocols.

## [resources permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#resources) Resources

- **GitHub Repository**: [spring-ai-agent-utils](https://github.com/spring-ai-community/spring-ai-agent-utils)
- **TaskTools Documentation**: [TaskTools.md](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/TaskTools.md)
- **Example Project**: [subagent-demo](https://github.com/spring-ai-community/spring-ai-agent-utils/tree/main/examples/subagent-demo)

#### [related permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#related) Related

- [Claude Code Subagents](https://platform.claude.com/docs/en/agent-sdk/subagents) \- Original inspiration

#### [series links permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#series-links) Series Links

- **Part 1**: [Agent Skills](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills) \- Modular, reusable capabilities
- **Part 2**: [AskUserQuestionTool](https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool) \- Interactive workflows
- **Part 3**: [TodoWriteTool](https://spring.io/blog/2026/01/20/spring-ai-agentic-patterns-3-todowrite/) \- Structured planning
- **Part 4**: Subagent Orchestration (this post) - Hierarchical agent architectures
- **Part 5**: [A2A Integration](https://spring.io/blog/2026/01/29/spring-ai-agentic-patterns-a2a-integration) \- Building interoperable agents with the Agent2Agent protocol
- **Part (soon)**: Subagent Extension Framework (coming soon) - Protocol-agnostic agent orchestration

#### [related spring ai blogs permalink](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents\#related-spring-ai-blogs) Related Spring AI Blogs

- [Dynamic Tool Discovery](https://spring.io/blog/2025/12/11/spring-ai-tool-search-tools-tzolov) \- Achieve 34-64% token savings
- [Tool Argument Augmentation](https://spring.io/blog/2025/12/23/spring-ai-tool-argument-augmenter-tzolov) \- Capture LLM reasoning during tool execution

Disqus Comments

We were unable to load Disqus. If you are a moderator please see our [troubleshooting guide](https://docs.disqus.com/help/83/).

- 1 comment
  - [1](https://disqus.com/home/notifications/)
  - [Login](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)
    - [Disqus](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)
    - [Facebook](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)
    - [X (Twitter)](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)
    - [Google](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)
    - [Microsoft](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)
    - [Apple](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)

G

Join the discussion??
嚜?
Comment

###### Log in with

###### or sign up with Disqus  or pick a name

### Disqus is a discussion network

- Don't be a jerk or do anything illegal. Everything is easier that way.

[Read full terms and conditions](https://docs.disqus.com/kb/terms-and-policies/)

This comment platform is hosted by Disqus, Inc. I authorize Disqus and its affiliates to:

- Use, sell, and share my information to enable me to use its comment services and for marketing purposes, including cross-context behavioral advertising, as described in our [Terms of Service](https://help.disqus.com/customer/portal/articles/466260-terms-of-service) and [Privacy Policy](https://disqus.com/privacy-policy), including supplementing that information with other data about me, such as my browsing and location data.
- Contact me or enable others to contact me by email with offers for goods or services
- Process any sensitive personal information that I submit in a comment. See our [Privacy Policy](https://disqus.com/privacy-policy) for more information

Acknowledge I am 18 or older

- [Favorite this discussion](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default# "Favorite this discussion")

  - ## Discussion Favorited!



    Favoriting means this is a discussion worth sharing. It gets shared to your followers' Disqus feeds, and gives the creator kudos!


     [Find More Discussions](https://disqus.com/home/?utm_source=disqus_embed&utm_content=recommend_btn)

[Share](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)

  - Tweet this discussion
  - Share this discussion on Facebook
  - Share this discussion via email
  - Copy link to discussion

  - [Best](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)
  - [Newest](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)
  - [Oldest](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)

- - [?(https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default# "Collapse")
  - [+](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default# "Expand")
  - [Flag as inappropriate](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default# "Flag as inappropriate")


C

[Chirag Moradiya](https://disqus.com/by/chirag_moradiya/)[23 days ago](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents#comment-6830142366 "Wednesday, January 28, 2026 7:40 AM")

Wow! I am excited to try this.

We were on a path to implement claude like Skills and Sub-Agents on Spring AI for [https://kerika.com](https://kerika.com/ "https://kerika.com") Now, route has been changed to try this.

see more

[0Press the down arrow key to see users who liked this](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default# "Vote up") [0Press the down arrow key to see users who disliked this](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default# "Vote down")

[Reply](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)
[Share ?榜(https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)

[Show more replies](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)

[Load more comments](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default#)

- [SubscribeSubscribed](https://disqus.com/embed/comments/?base=default&f=spring-io&t_i=c261c9a95d72dde0539d42264f9f82d9&t_u=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F27%2Fspring-ai-agentic-patterns-4-task-subagents&t_e=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_d=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&t_t=Spring%20AI%20Agentic%20Patterns%20(Part%204)%3A%20Subagent%20Orchestration&s_o=default# "Subscribe and get email updates from this discussion")

- [Privacy](https://disqus.com/privacy-policy "Privacy")
- [Do Not Sell My Data](https://disqus.com/data-sharing-settings/)

[Powered by Disqus](https://disqus.com/ "Powered by Disqus")

## Get the Spring newsletter

Stay connected with the Spring newsletter

[Subscribe](https://go-vmware.broadcom.com/tnz-spring-newsletter-subscribe)

![](https://spring.io/img/extra/footer.svg)

## Get ahead

VMware offers training and certification to turbo-charge your progress.

[Learn more](https://spring.academy/)

## Get support

Tanzu Spring offers support and binaries for OpenJDK?? Spring, and Apache Tomcat簧 in one simple subscription.

[Learn more](https://spring.io/support)

## Upcoming events

Check out all the upcoming events in the Spring community.

[View all](https://spring.io/events)

[Why Spring](https://spring.io/why-spring)

[Generative AI](https://spring.io/ai)

[Microservices](https://spring.io/microservices)

[Reactive](https://spring.io/reactive)

[Event Driven](https://spring.io/event-driven)

[Cloud](https://spring.io/cloud)

[Web Applications](https://spring.io/web-applications)

[Serverless](https://spring.io/serverless)

[Batch](https://spring.io/batch)

[Learn](https://spring.io/learn)

[Quickstart](https://spring.io/quickstart)

[Guides](https://spring.io/guides)

[Courses](https://spring.academy/courses)

[Get Certified](https://spring.academy/learning-path)

[Projects](https://spring.io/projects)

Resources

[Blog](https://spring.io/blog)

[Release Calendar](https://spring.io/projects#release-calendar)

[Version Mappings](https://spring.io/projects/generations)

[Security Advisories](https://spring.io/security)

[Community](https://spring.io/community)

[Events](https://spring.io/events)

[Authors](https://spring.io/authors)

[Enterprise](https://enterprise.spring.io/)

[Long-term Support](https://enterprise.spring.io/lts-releases)

[Automated Upgrades](https://enterprise.spring.io/spring-application-advisor)

[Governance and Compliance](https://enterprise.spring.io/enterprise-extensions)

[Modern App Development](https://enterprise.spring.io/enterprise-components)

[Thank You](https://spring.io/thank-you)

Copyright 穢 2005 - 2026 Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.

[Terms of Use](https://www.broadcom.com/company/legal/terms-of-use) ??[Privacy](https://www.broadcom.com/company/legal/privacy) ??[Trademark Guidelines](https://spring.io/trademarks)

Apache簧, Apache Tomcat簧, Apache Kafka簧, Apache Cassandra?? and Apache Geode??are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java?? Java??SE, Java??EE, and OpenJDK??are trademarks of Oracle and/or its affiliates. Kubernetes簧 is a registered trademark of the Linux Foundation in the United States and other countries. Linux簧 is the registered trademark of Linus Torvalds in the United States and other countries. Windows簧 and Microsoft簧 Azure are registered trademarks of Microsoft Corporation. ?WS??and ?mazon Web Services??are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners.

[Youtube](https://www.youtube.com/user/SpringSourceDev)[Github](https://github.com/spring-projects)[X](https://x.com/springcentral)[BlueSky](https://bsky.app/profile/spring.io)
