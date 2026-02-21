![蝔???(https://lf-web-assets.juejin.cn/obj/juejin-web/xitu_juejin_web/e08da34488b114bd4c665ba2fa520a31.svg)![蝔???(https://lf-web-assets.juejin.cn/obj/juejin-web/xitu_juejin_web/6c61ae65d1c41ae8221a670fa32d05aa.svg)

- 擐△

  - [擐△](https://juejin.cn/)
  - [瘝貊](https://juejin.cn/pins)
  - [霂曄?](https://juejin.cn/course)
  - [?唳?釣\\
     HOT](https://aidp.juejin.cn/)
  - [AI Coding](https://aicoding.juejin.cn/)
  - ?游?
    - [?湔](https://juejin.cn/live)
    - [瘣餃](https://juejin.cn/events/all)
    - [APP](https://juejin.cn/app?utm_source=jj_nav)
    - [?辣](https://juejin.cn/extension?utm_source=jj_nav)
  - [?湔](https://juejin.cn/live)
  - [瘣餃](https://juejin.cn/events/all)
  - [APP](https://juejin.cn/app?utm_source=jj_nav)
  - [?辣](https://juejin.cn/extension?utm_source=jj_nav)

  - ?揣?
      皜征


  - ???葉敹?









    - ??蝡?
    - ?硫??
    - ??霈?
    - ?誨??
    - ?阮蝞?

???菜?
?亦??游?
  - ?餃?






瘜典?

# SpringAI Agent撘??蝐?霈姐avaer銋隞亦銝gent Skills

[銝?啁](https://juejin.cn/user/377887729916126/posts)

2026-01-27

407

?粉12??


> [?隡?AI撘??SpringAI Agent + Skills?摰??箄摨](https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2FujxVleNhjxzUgL-rjfFcVA "https://mp.weixin.qq.com/s/ujxVleNhjxzUgL-rjfFcVA")

閬秩?餈I?詨霂?銝凋?銋??恬?瘥急????**Claude Skills**嚗悟???圈??????臬?銝箔?銋???SpringAI撅撌脩?餈??Skills鈭?餈?????芣???鈭?
靚秩AI?嗡誨java撘????鈭? ??嗡?銝摰?敺?嚗??臬???惜餈憒亙戎??
?乩??交?隞祇??遣銝銝泌ode reviewer, ?亙???撉???憒?撠pringAI?kills蝏?韏瑟雿輻

## 銝?★?桀?撱?
### 1\. ?箇??臬?閬?

閬?撉pringAI & Skills嚗??閬?蝥批SpringAI 2.x?嚗??嗆?隞祉?SpringBoot銋隞亙?蝥批4.x

- SpringAI: 2.0.0-M2
- JDK21
- SpringBoot: 4.0.1

?支?餈?銝芸?砌?韏?憭??賑?臭誑?銝銝芣?unction Tool?之璅∪??乩?銝箄?銝芸??啁?憭扯?銝剜

?賑餈???箄停?之璅∪?`GLM-4.5-Flash` 嚗??停?臬?銝箏??晶嚗???餈?嚗笆??雿???隡撈瘝⊥?隞颱?憸???嚗?
### 2\. 憿寧?遣

?乩??交?隞砍?撱箔?銝杵pringAI摨嚗笆鈭?銝芣???SpringAI摨嚗`pom.xml`?蔭銝哨?雿??銝餈??箇????嚗?銝芯?瘝∩?銋末霂渡?

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.1</version>
    <relativePath />
</parent>
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <spring-ai.version>2.0.0-M2</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>

<repositories>
    <repository>
        <id>spring-snapshots</id>
        <name>Spring Snapshots</name>
        <url>https://repo.spring.io/snapshot</url>
        <releases>
            <enabled>false</enabled>
        </releases>
    </repository>
    <repository>
        <name>Central Portal Snapshots</name>
        <id>central-portal-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

?乩??仿??寧?銝銝?隞祈?銝芷★?格??典??銝芣敹?韏?
```xml
<dependencies>
    <dependency>
        <groupId>org.springaicommunity</groupId>
        <artifactId>spring-ai-agent-utils</artifactId>
        <version>0.4.1</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-zhipuai</artifactId>
    </dependency>
</dependencies>
```

- spring-ai-agent-utils: 餈葵撠望SpringAI餈?agent撘???喲靘???- spring-ai-starter-model-zhipuai: 餈葵?舀靚勗之璅∪?餈?鈭支???韏?

### 3\. 憿寧?蔭

靘???銋?嚗銝撠望?券?蝵格?隞嗡葉嚗?蝵夏LM霈輸??喃縑?胯誑?gent?詨?蔭?嚗笆摨??蔭?辣 `resources/application.yml`

```yaml
spring:
  ai:
    zhipuai:
      # api-key 雿輻雿撌梁霂瑞?餈??踵嚗??蛹鈭??刻?嚗隞仿??臬?餈?霈曄蔭
      api-key: ${zhipuai-api-key}
      chat: # ?予璅∪?
        options:
          model: GLM-4.5-Flash

## Agent Configuration
agent:
  skills:
    dirs: classpath:/.claude/skills
  model: GLM-4.5-Flash
```

餈?銝芷?蝵桃?韏瑟???pringAI?詨?僎瘝⊥?憭芸???恬??嗡葉 `agent` ?詨??蝵桐葉嚗蜓閬挽蝵桐?skills???曇楝敺?雿輻?odel

?寞銝??銋??賑撠kills靽⊥嚗?灼resources/.claude/skills`?桀?銝?
?啣?銝銝芰敶code-reviewer`嚗敶???隞嗡蛹 `SKILL.md`

```bash
.claude/skills/code-reviewer/
??? SKILL.md
```

撖孵???摰孵?銝?
```markdown
---
name: code-reviewer
description: Reviews Java code for best practices, security issues, and Spring Framework conventions. Use when user asks to review, analyze, or audit code.
---

# Code Reviewer

## Instructions

?典恣?乩誨?嚗?
1. 璉?交?血??典??冽?瘣?憒QL瘜典?SS蝑?
2. 撉??臬?萄儐鈭pring Boot??雿喳?頝蛛?憒迤蝖桐蝙?杓Service?Repository蝑釣閫??
3. ?交瞏?征??撘虜
4. ???隞???航粉?批??舐輕?斗抒?撱箄悅
5. ???瑚?????嚗僎??隞??蝷箔?
6. 隞乩葉???孵?餈?隞??霂恣蝏?
```

### 4\. Skills蝞閬秩??
?賑銝?kill瘥?蝞??撠望銝銝沸arkdown?﹝嚗pringAI?舀??kills銝哨??支???箸?SKILL.md`?辣嚗??怠??唳嚗?蝘啣??膩嚗誑??撖潔誨??雿銵摰遙?∠?霂湔?嚗?憭?餈隞交??詨???研芋?踹?????
銝銝芸虜閫?skills蝏?憒?

```perl
my-skill/
??? SKILL.md          # Required: instructions + metadata
??? scripts/          # Optional: executable code
??? references/       # Optional: documentation
??? assets/           # Optional: templates, resources
```

## 鈭敹???
?啣?蔭??撌脩?摰?嚗銝撘憪迤撘?雿???
### 2.1 鈭支??亙?? MyLoggingAdvisor

銝箔?霈拍頂蝏?憭扳芋???渡?鈭支??湔??堆??賑撠??嫣漱鈭??亙?餈??游?憟賜??嚗?憿箔噶??銝?銝甈∠?瑟??亦??桃?餈?銝哨?摰?銝??活鈭支?嚗?
```java
public class MyLoggingAdvisor implements BaseAdvisor {
    private final int order;

    public final boolean showSystemMessage;

    public final boolean showAvailableTools;

    private AtomicInteger cnt = new AtomicInteger(1);

    private MyLoggingAdvisor(int order, boolean showSystemMessage, boolean showAvailableTools) {
        this.order = order;
        this.showSystemMessage = showSystemMessage;
        this.showAvailableTools = showAvailableTools;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        System.out.println("======================= 蝚?" + cnt.getAndAdd(1) + " 頧?====================================");

        StringBuilder sb = new StringBuilder("\nUSER: ");

        if (this.showSystemMessage && chatClientRequest.prompt().getSystemMessage() != null) {
            sb.append("\n - SYSTEM: ").append(first(chatClientRequest.prompt().getSystemMessage().getText(), 300));
        }

        if (this.showAvailableTools) {
            Object tools = "No Tools";

            if (chatClientRequest.prompt().getOptions() instanceof ToolCallingChatOptions toolOptions) {
                tools = toolOptions.getToolCallbacks().stream().map(tc -> tc.getToolDefinition().name()).toList();
            }

            sb.append("\n - TOOLS: ").append(ModelOptionsUtils.toJsonString(tools));
        }

        Message lastMessage = chatClientRequest.prompt().getLastUserOrToolResponseMessage();

        if (lastMessage.getMessageType() == MessageType.TOOL) {
            ToolResponseMessage toolResponseMessage = (ToolResponseMessage) lastMessage;
            for (var toolResponse : toolResponseMessage.getResponses()) {
                var tr = toolResponse.name() + ": " + first(toolResponse.responseData(), 1000);
                sb.append("\n - TOOL-RESPONSE: ").append(tr);
            }
        } else if (lastMessage.getMessageType() == MessageType.USER) {
            if (StringUtils.hasText(lastMessage.getText())) {
                sb.append("\n - TEXT: ").append(first(lastMessage.getText(), 1000));
            }
        }

        System.out.println("before: " + sb);
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        StringBuilder sb = new StringBuilder("\nASSISTANT: ");

        if (chatClientResponse.chatResponse() == null || chatClientResponse.chatResponse().getResults() == null) {
            sb.append(" No chat response ");
            System.out.println("after: " + sb);
            return chatClientResponse;
        }

        for (var generation : chatClientResponse.chatResponse().getResults()) {
            var message = generation.getOutput();
            if (message.getToolCalls() != null) {
                for (var toolCall : message.getToolCalls()) {
                    sb.append("\n - TOOL-CALL: ")
                            .append(toolCall.name())
                            .append(" (")
                            .append(toolCall.arguments())
                            .append(")");
                }
            }

            if (message.getText() != null) {
                if (StringUtils.hasText(message.getText())) {
                    sb.append("\n - TEXT: ").append(first(message.getText(), 1200));
                }
            }
        }

        System.out.println("after: " + sb);
        return chatClientResponse;
    }

    private String first(String text, int n) {
        if (text.length() <= n) {
            return text;
        }
        return text.substring(0, n) + "...";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int order = 0;

        private boolean showSystemMessage = true;

        private boolean showAvailableTools = true;

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder showSystemMessage(boolean showSystemMessage) {
            this.showSystemMessage = showSystemMessage;
            return this;
        }

        public Builder showAvailableTools(boolean showAvailableTools) {
            this.showAvailableTools = showAvailableTools;
            return this;
        }

        public MyLoggingAdvisor build() {
            MyLoggingAdvisor advisor = new MyLoggingAdvisor(this.order, this.showSystemMessage,
                    this.showAvailableTools);
            return advisor;
        }
    }
}
```

### 2.2 ???其?霂恣?誨??
?賑?湔雿輻 [摰? \| ?嗅蝖?剖遣?亥?摨蝑?其犖嚗鈭pringAI+RAG???游??財(https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2FNHqLJbos-_nrxNNmhg7IBQ "https://mp.weixin.qq.com/s/NHqLJbos-_nrxNNmhg7IBQ") 銝剔?隞??????摰嫣?銝箏?霂恣??摰對???餈挾蝞?????撌亙隡?摰∪隞銋?摰?
```java
package com.git.hui.springai.app.demo;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * ?﹝??撌亙蝐? * 撠?﹝???撠???隞乩噶?游末?啗?銵?????蝝? */
public class DocumentChunker {

    private final int maxChunkSize;
    private final int overlapSize;

    public static DocumentChunker DEFAULT_CHUNKER = new DocumentChunker();

    public DocumentChunker() {
        this(500, 50); // 暺恕?潘??憭批?憭批?500銝芸?蝚佗???50銝芸?蝚?    }

    public DocumentChunker(int maxChunkSize, int overlapSize) {
        this.maxChunkSize = maxChunkSize;
        this.overlapSize = overlapSize;
    }

    /**
     * 撠?獢???脫???     *
     * @param document 颲?﹝
     * @return ????﹝??銵?     */
    public List<Document> chunkDocument(Document document) {
        String content = document.getText();
        if (content == null || content.trim().isEmpty()) {
            return List.of(document);
        }

        List<String> chunks = splitText(content);
        List<Document> chunkedDocuments = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String chunkId = document.getId() + "_chunk_" + i;

            // ?遣?啁??﹝??靽????﹝???唳
            Document chunkDoc = new Document(chunkId, chunk, new java.util.HashMap<>(document.getMetadata()));

            // 瘛餃???喟????            chunkDoc.getMetadata().put("chunk_index", i);
            chunkDoc.getMetadata().put("total_chunks", chunks.size());
            chunkDoc.getMetadata().put("original_document_id", document.getId());

            chunkedDocuments.add(chunkDoc);
        }

        return chunkedDocuments;
    }

    /**
     * 撠??砍??脫???     *
     * @param text 颲?
     * @return ??????銵?     */
    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();

        // ??蝘??泵?嚗??霂凋?颲寧?憭??莎??銝剜??亙??瑯??孵蝑?
        String[] sentences = text.split("(?<=??|(?<=嚗?|(?<=!)|(?<=嚗?|(?<=\\?)|(?<=\\n\\n)");

        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            // 頝唾?蝛箏摮?            if (sentence.trim().isEmpty()) {
                continue;
            }

            // 憒?敶???銝?亙?銝?餈?憭批之撠?撠望溶?敶???            if (currentChunk.length() + sentence.length() <= maxChunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(sentence);
                } else {
                    currentChunk.append(sentence);
                }
            } else {
                // 憒?敶??蛹蝛綽?雿?葵?亙?憭芷嚗?閬撩?嗅???                if (currentChunk.length() == 0) {
                    List<String> subChunks = forceSplit(sentence, maxChunkSize);
                    for (int i = 0; i < subChunks.size(); i++) {
                        String subChunk = subChunks.get(i);
                        // 憒?銝???銝芸???瘛餃??啣???撟嗡?摮?                        if (i < subChunks.size() - 1) {
                            chunks.add(subChunk);
                        } else {
                            currentChunk.append(subChunk);
                        }
                    }
                } else {
                    // 靽?敶???                    chunks.add(currentChunk.toString());
                    // 撘憪??????典?
                    currentChunk = new StringBuilder();

                    // 瘛餃????典?嚗??摮摨血之鈭??之撠???撠暸??                    if (sentence.length() > overlapSize) {
                        String overlap = sentence.substring(Math.max(0, sentence.length() - overlapSize));
                        currentChunk.append(overlap);
                        currentChunk.append(sentence);
                    } else {
                        currentChunk.append(sentence);
                    }
                }
            }
        }

        // 瘛餃????銝芸?
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    /**
     * 撘箏撠????摰之撠???     *
     * @param text 颲?
     * @param maxSize ?憭批?憭批?
     * @return ??????銵?     */
    private List<String> forceSplit(String text, int maxSize) {
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());
            String chunk = text.substring(start, end);
            chunks.add(chunk);
            start = end;
        }

        return chunks;
    }

    /**
     * 撠?銝芣?獢???怠??脫???     *
     * @param documents 颲?﹝?”
     * @return ????﹝??銵?     */
    public List<Document> chunkDocuments(List<Document> documents) {
        List<Document> allChunks = new ArrayList<>();

        for (Document document : documents) {
            allChunks.addAll(chunkDocument(document));
        }

        return allChunks;
    }
}
```

### 2.3 ?詨?摰

?蔭Agent摰隞??霂恣

**Bean摰?銝?韏釣??*

- CommandLineRunner: Spring?臬??冽銵??亙
- ChatClient.Builder: ?其??遣?予摰Ｘ蝡?- @Value("${agent.skills.dirs:Unknown}"): 瘜典?蔭撅改??瑕???賜敶?皞?銵?
**ChatClient?蔭??*

- 蝟餌??內霂?蝵殷?
- ??賢極?琿?蝵殷?
  - SkillsTool.builder().addSkillsResources(agentSkillsDirs).build(): ?冽?頧賡?摰????質?皞?  - FileSystemTools.builder().build(): ???辣蝟餌?霈輸?賢?
  - ShellTools.builder().build(): ???賭誘銵銵??- Advisor?蔭:
  - ToolCallAdvisor.builder().build(): 憭?撌亙靚?餉?
  - MyLoggingAdvisor.builder().showAvailableTools(false).showSystemMessage(false).build(): ?芸?銋敹扇敶???撌亙?頂蝏??航祕??
**隞??霂恣?扯?瘚?**

霂瑟??扯?

- prompt(): ?遣?內霂?- .call(): ?絲AI霂瑟?
- .content(): ?瑕?餈?蝏?

```java
@Bean
CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder,
                                    @Value("${agent.skills.dirs:Unknown}") List<Resource> agentSkillsDirs) throws IOException {

    return args -> {

        ChatClient chatClient = chatClientBuilder // @formatter:off
                .defaultSystem("憪?餈?唳???賢??拍?瑟說頞喳閬?.")

                // Skills tool
                .defaultToolCallbacks(SkillsTool.builder().addSkillsResources(agentSkillsDirs).build())
                // ?舀?霂餃?蝟餌??辣?捆嚗鈭粉??隞祇?閬?摰∠?隞??
                .defaultTools(FileSystemTools.builder().build())
                // ?舀??扯??嚗??kills銝剖??究cript嚗銋?鈭??祉??扯?嚗??停?臬?
                .defaultTools(ShellTools.builder().build())

                .defaultAdvisors(
                        // Tool Calling advisor
                        ToolCallAdvisor.builder().build(),
                        // Custom logging advisor
                        MyLoggingAdvisor.builder()
                                .showAvailableTools(false)
                                .showSystemMessage(false)
                                .build())
                .build();
        // @formatter:on

        var answer = chatClient
                // 銝?瑚??誨??蝵殷?霂瑟?桀???雿蔭餈??踵
                .prompt("""
                        ??雿喳????孵?嚗?摰∩??Ｙ?隞??摰:

                         D:\\Workspace\\hui\\project\\spring-ai-demo\\v2\\T01-agentic-skills-simple-design\\src\\main\\java\\com\\git\\hui\\springai\\app\\demo\\DocumentChunker.java
                         """)
                .call()
                .content();

        System.out.println("The Answer: " + answer);
    };
}
```

### 2.4 ?扯?瞍內

?乩??交?隞砍?券★?殷?撉?銝銝???雿??典?典隞方??銝哨??蔭銝之璅∪??pi-key嚗??嗡??臭誑?湔?肘ml?蔭?辣銝剛?銵?蝵殷?

```java
@Slf4j
@SpringBootApplication
public class T01Application {

    public static void main(String[] args) {
        SpringApplication.run(T01Application.class, args);
    }
}
```

![](https://p6-xtjj-sign.byteimg.com/tos-cn-i-73owjymdk6/bc72c6c5b56542f4ac76fc43dddb45fc~tplv-73owjymdk6-jj-mark-v1:0:0:0:0:5o6Y6YeR5oqA5pyv56S-5Yy6IEAg5LiA54Gw54Gw:q75.awebp?rk3s=f64ab15b&x-expires=1771984044&x-signature=CXsPEDC8qkFIJcRSw2fl8QF8Cmk%3D)

![](https://p6-xtjj-sign.byteimg.com/tos-cn-i-73owjymdk6/8dd829f7048640d8949f8aefdc516218~tplv-73owjymdk6-jj-mark-v1:0:0:0:0:5o6Y6YeR5oqA5pyv56S-5Yy6IEAg5LiA54Gw54Gw:q75.awebp?rk3s=f64ab15b&x-expires=1771984044&x-signature=2%2FFw27O1waOEOuVPMqOtDlr8F8s%3D)

隞??Ｙ??芸銋隞亦??綽?蝟餌?銝之璅∪?銋餈?鈭?頧桀笆霂?

1. 蝚砌?頧殷? ?冽??憪?瘙?-\> 憭扳芋??
- 憭扳芋???怠?閬?銵誨??摰∴?蝏頂蝏?摨?`{"command":"code-reviewer"}`

2. 蝚砌?頧殷?蝟餌??交?啣?摨???霂餃?skills?漲??銵?摨?
- 憭扳芋????閬粉??閬?摰∠?隞???捆嚗笆摨???銝?`{"filePath":"D:\\Workspace\\hui\\project\\spring-ai-demo\\v2\\T01-agentic-skills-simple-design\\src\\main\\java\\com\\git\\hui\\springai\\app\\demo\\DocumentChunker.java"}`

3. 蝚砌?頧殷?蝟餌?霂餃?隞???捆嚗???憭扳芋??
- 憭扳芋????摰∠???
銝?臬??渡?餈??嚗??渲閎??隡撈?臭誑??

```markdown
?箔?撖嫣誨??霂衣???嚗?銝箸??隞乩?隞??霂恣?亙?嚗?
## ?? 隞??霂恣?亙? - DocumentChunker.java

### ??**隡**

1. **?摰?批撩**嚗誨???唬?摰??獢?????踝??嚗?   - ??獢????   - ?寥??﹝??
   - ?箄霂凋?颲寧??
   - ???箏
   - ?踵??砍撩?嗅???
2. **?臬末??獢?釣??*嚗掩?瘜???啁?銝剜?瘜券?嚗秩????券?
3. **????霈文?*嚗?靘?????霈文??堆?maxChunkSize=500, overlapSize=50嚗?
4. **??桐???*嚗迤蝖桐??????﹝???唳嚗僎瘛餃?鈭??詨???唳

### ?? **?閬餈??桅?**

#### 1. **蝥輻?摰?桅?**

// ?桅?嚗???靘??舐瑪蝔??函?
public static DocumentChunker DEFAULT_CHUNKER = new DocumentChunker();

**撱箄悅**嚗蝙??`volatile` ??`AtomicReference`嚗??宏?日???靘悟?冽?芸楛蝞∠???
#### 2. **?撉?銝雲**

// ?桅?嚗???唳瓷???圈?霂?public DocumentChunker(int maxChunkSize, int overlapSize) {
    this.maxChunkSize = maxChunkSize;
    this.overlapSize = overlapSize;
}

**撱箄悅**嚗溶???圈?霂?蝖桐? `maxChunkSize > 0` 銝?`overlapSize < maxChunkSize`??
#### 3. **甇??銵刻噢撘??漲**

// ?桅?嚗迤?”颲曉?餈?憭?嚗隞亦輕??String[] sentences = text.split("(?<={?)|(?<=嚗?|(?<=!)|(?<=嚗?|(?<=\\?)|(?<=\\n\\n)");

**撱箄悅**嚗??蛹撣賊??蝙?冽皜?撘???霂剛???蝚艾?
#### 4. **?扯隡?蝛粹**

// ?桅?嚗tringBuilder???臭誑隡?
if (currentChunk.length() > 0) {
    currentChunk.append(sentence);
} else {
    currentChunk.append(sentence);
}

**撱箄悅**嚗??蛹 `currentChunk.append(sentence);`

#### 5. **撘虜憭?蝻箏仃**
隞??銝剜瓷????賜?撘虜?嚗?嚗?- ??銝雲
- ?﹝餈之撖潸?扯?桅?

### ? **?瑚??寡?撱箄悅**

#### 1. **瘛餃??撉?**

public DocumentChunker(int maxChunkSize, int overlapSize) {
    if (maxChunkSize <= 0) {
        throw new IllegalArgumentException("maxChunkSize must be positive");
    }
    if (overlapSize >= maxChunkSize) {
        throw new IllegalArgumentException("overlapSize must be less than maxChunkSize");
    }
    this.maxChunkSize = maxChunkSize;
    this.overlapSize = overlapSize;
}

#### 2. **蝥輻?摰?寡?**

public static final DocumentChunker DEFAULT_CHUNKER = new DocumentChunker();
// ?宏?日???靘?霈拍?瑁撌梁恣??
#### 3. **??撣賊?**

private static final String SENTENCE_SEPARATOR_PATTERN =
    "(?<={?)|(?<=嚗?|(?<=!)|(?<=嚗?|(?<=\\?)|(?<=\\n\\n)";

#### 4. **瘛餃?Builder璅∪?**
??雿輻Builder璅∪??交?菜暑?圈?蝵桀??啜?
#### 5. **?扯隡?**

// 隡?StringBuilder??
private List<String> splitText(String text) {
    List<String> chunks = new ArrayList<>();
    String[] sentences = text.split(SENTENCE_SEPARATOR_PATTERN);
    StringBuilder currentChunk = new StringBuilder();

    for (String sentence : sentences) {
        if (sentence.trim().isEmpty()) continue;

        if (currentChunk.length() + sentence.length() <= maxChunkSize) {
            currentChunk.append(sentence);
        } else {
            // 憭????脤餉?
        }
    }
    // ...
}

### ? **?颱?霂遠**

餈銝銝芸??賢??氬挽霈∪????﹝??撌亙蝐鳴?銝餉??桅??葉?函瑪蝔??具??圈?霂?隞???舐輕?斗扳?Ｕ?銝膩?寡?嚗隞交???誨???亙ㄝ?批??舐輕?斗扼?
**撱箄悅隡?蝥?*嚗?1. 擃??漣嚗??圈?霂瑪蝔???2. 銝凋??漣嚗扯隡??誨????3. 雿??漣嚗uilder璅∪???撣詨???```

## 銝?蝏?
SpringAI?gent撘??撘??kills?箏嚗?撣貊??停摰鈭I摨?極蝔??銝芾?蝔??唬??伐??冽?餈瘥?雿???銝?銝???憯堆?`Spring???`

摰?孵??賜蝞??雿餈葵???挽霈∪摮艾??輕?蓮??餈敺澆??賑摮虫????AI?嗡誨嚗?雿??賑?唳????踝?憒憭?芋???挽霈∠?嚗蓮?憭扳芋???典???餈?賣?賑瘥?銝注?扳隞ε蝔???憭抒?韐Ｗ???
???餈?憟??摰?嚗?
![](https://p6-xtjj-sign.byteimg.com/tos-cn-i-73owjymdk6/5dc77a4aac3c4c9da90d93f6be1fa000~tplv-73owjymdk6-jj-mark-v1:0:0:0:0:5o6Y6YeR5oqA5pyv56S-5Yy6IEAg5LiA54Gw54Gw:q75.awebp?rk3s=f64ab15b&x-expires=1771984044&x-signature=yLhqRW7XfobWcVYZVymin%2BGkKjA%3D)

Spring AI??箔?撌亙???瘜???摰??撌亙嚗蝙隞颱?LLM?質???扯?嚗kills??銵?蝔??虜?臭??Ｖ?甇伐?

1. ?嚗?券畾蛛?

- ??`SKILL.md`?辣銝剔???殷?敹恍??唳??賜?摰?瘜典?

2. 霂凋??寥?嚗笆霂?蝔葉嚗?
- 敶?瑕??箄窈瘙嚗LM 隡??亙極?瑕?銋葉撋???賣?餈啜???LLM ?斗?冽霂瑟??刻祗銋?銝?銝芣??賜??膩?寥?嚗?隡??刻砲??賢極?瘀?撟嗅???賢?蝘唬?銝箏??唬???摰?
3. ?扯?嚗??質??冽嚗?
- 敶??冽??賢極?瑟嚗killsTool隡?蝤??蝸摰?KILL.md?捆嚗僎撠銝??賜??箇??桀?頝臬?銝韏瑁???憭批?霂剛?璅∪?嚗LM嚗??LLM隡??扳??賢?摰嫣葉??隞斗銵????賢??其??嗡??辣???抵??穿?LLM隡蝙?灼FileSystemTools`?Read`?賣?ShellTools`?Bash`?賣?交??霈輸摰賑

* * *

憿寧皞?嚗?
- [github.com/liuyueyi/sp?因(https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fliuyueyi%2Fspring-ai-demo%2Ftree%2Fmaster%2Fv2%2FT01-agentic-skills-simple-design "https://github.com/liuyueyi/spring-ai-demo/tree/master/v2/T01-agentic-skills-simple-design")

?嗅蝖?仿嚗?
- [LLM 摨撘?隞銋??嗅蝖銋隞亥粉??蝘??????](https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2FqCn8x2XO2shA8MheYbHq0w "https://mp.weixin.qq.com/s/qCn8x2XO2shA8MheYbHq0w")
- [憭扳芋???典??頂??蝔?摨?銝箔?銋?????LLM??雿?銝憭?摨嚗(https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2F2GXBNOUq3jlysipftz8TpA "https://mp.weixin.qq.com/s/2GXBNOUq3jlysipftz8TpA")
- [憭扳芋???典??頂??蝔?蝚砌?蝡LM?啣??典?隞銋?](https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2Fv-z6EHY300ElOxdGPdzc0w "https://mp.weixin.qq.com/s/v-z6EHY300ElOxdGPdzc0w")
- [憭扳芋???典??頂??蝔?蝚砌?蝡?璅∪?銝?嚗??唳??臭??迤??園?瓢(https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2Ft_BuAW9i0npcaJdua3Am2Q "https://mp.weixin.qq.com/s/t_BuAW9i0npcaJdua3Am2Q")
- [憭扳芋???典??頂??蝔?蝚砌?蝡?銝箔?銋??rompt銵函敺?嚗(https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2Fvzt0bGwcfnASOiBa0Kc7VQ "https://mp.weixin.qq.com/s/vzt0bGwcfnASOiBa0Kc7VQ")
- [憭扳芋???典??頂??蝔?蝚砍?蝡rompt ?極蝔?蝏?霈曇恣](https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2FNk-N34TLJVCTI5F4k5rGaQ "https://mp.weixin.qq.com/s/Nk-N34TLJVCTI5F4k5rGaQ")
- [憭扳芋???典??頂??蝔?蝚砌?蝡?隞?Prompt ??Prompt 璅⊥銝極蝔祥?(https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2FZQbztqBq7_PzynG06N4-mg "https://mp.weixin.qq.com/s/ZQbztqBq7_PzynG06N4-mg")
- [憭扳芋???典??頂??蝔?蝚砍蝡?銝????????颲寧?](https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2FnnKspRO87xbrn4-LBV3RNA "https://mp.weixin.qq.com/s/nnKspRO87xbrn4-LBV3RNA")
- [憭扳芋???典??頂??蝔?蝚砌?蝡?隞???銝??????恣??銝??(https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2F_5D2tF6CPnafj5mlmlwLNw "https://mp.weixin.qq.com/s/_5D2tF6CPnafj5mlmlwLNw")

* * *

摰?

- [憭扳芋???典?????銝斤銵??唬?銝芾?嗉祗閮?啣????箄雿(https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2F96rHyp_gBUgmA2dhSbzNww "https://mp.weixin.qq.com/s/96rHyp_gBUgmA2dhSbzNww")
- [憭扳芋???典??????箔?SpringAI銝之璅∪???蔭?巨?箄???嗆?](https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2FSnXdTB6tYqAzG7HgbnTSAQ "https://mp.weixin.qq.com/s/SnXdTB6tYqAzG7HgbnTSAQ")
- [摰? \| ?嗅蝖?剖遣?亥?摨蝑?其犖嚗鈭pringAI+RAG???游??財(https://link.juejin.cn/?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%2FNHqLJbos-_nrxNNmhg7IBQ "https://mp.weixin.qq.com/s/NHqLJbos-_nrxNNmhg7IBQ")

??

- [Spring AI Agentic Patterns (Part 1): Agent Skills - Modular, Reusable Capabilities](https://link.juejin.cn/?target=https%3A%2F%2Fspring.io%2Fblog%2F2026%2F01%2F13%2Fspring-ai-generic-agent-skills "https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills")

?倌嚗?
[?垢](https://juejin.cn/tag/%E5%90%8E%E7%AB%AF) [Agent](https://juejin.cn/tag/Agent) [LLM](https://juejin.cn/tag/LLM)

![](https://lf-web-assets.juejin.cn/obj/juejin-web/xitu_juejin_web/c12d6646efb2245fa4e88f0e1a9565b7.svg)

![](https://lf-web-assets.juejin.cn/obj/juejin-web/xitu_juejin_web/336af4d1fafabcca3b770c8ad7a50781.svg)

![](https://lf-web-assets.juejin.cn/obj/juejin-web/xitu_juejin_web/3d482c7a948bac826e155953b2a28a9e.svg)

?葵?單釣嚗移敶拇?唬???~

![avatar](https://p9-passport.byteacctimg.com/img/user-avatar/6dcf020f0c6feb9fc46635ca1adb478c~40x40.awebp)

?單釣


[![avatar](https://lf-web-assets.juejin.cn/obj/juejin-web/xitu_juejin_web/58aaf1326ac763d8a1054056f3b7f2ef.svg)\\
\\
銝?啁\\
![??蝑漣LV.5](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8584543d8535435a9d74c1fbf7901ac7~tplv-k3u1fbpfcp-jj:0:0:0:0:q75.avis)\\
\\
韏楛?祈?撌乓??啁blog?(https://juejin.cn/user/377887729916126/posts)

[267\\
\\
\\
??](https://juejin.cn/user/377887729916126/posts) [911k\\
\\
\\
?粉](https://juejin.cn/user/377887729916126/posts) [2.4k\\
\\
\\
蝎?](https://juejin.cn/user/377887729916126/followers)

?葵?單釣嚗移敶拇?唬???~

?單釣


撌脣瘜?

[蝘縑](https://juejin.cn/notification/im?participantId=377887729916126)

?桀?

?嗉絲

- [銝?★?桀?撱榜(https://juejin.cn/post/7599929684021837843#heading-0 "銝?★?桀?撱?)

  - [1\. ?箇??臬?閬?](https://juejin.cn/post/7599929684021837843#heading-1 "1. ?箇??臬?閬?")

  - [2\. 憿寧?遣](https://juejin.cn/post/7599929684021837843#heading-2 "2. 憿寧?遣")

  - [3\. 憿寧?蔭](https://juejin.cn/post/7599929684021837843#heading-3 "3. 憿寧?蔭")

  - [4\. Skills蝞閬秩?(https://juejin.cn/post/7599929684021837843#heading-4 "4. Skills蝞閬秩??)
- [鈭敹??財(https://juejin.cn/post/7599929684021837843#heading-5 "鈭敹???)

  - [2.1 鈭支??亙?? MyLoggingAdvisor](https://juejin.cn/post/7599929684021837843#heading-6 "2.1 鈭支??亙?? MyLoggingAdvisor")

  - [2.2 ???其?霂恣?誨?(https://juejin.cn/post/7599929684021837843#heading-7 "2.2 ???其?霂恣?誨??)

  - [2.3 ?詨?摰](https://juejin.cn/post/7599929684021837843#heading-8 "2.3 ?詨?摰")

  - [2.4 ?扯?瞍內](https://juejin.cn/post/7599929684021837843#heading-9 "2.4 ?扯?瞍內")
- [銝?蝏(https://juejin.cn/post/7599929684021837843#heading-10 "銝?蝏?)


?揣撱箄悅

?曉笆撅?雿???臬?摮?
????蝢扎??亙??孵凝靽∠黎

![](https://lf-web-assets.juejin.cn/obj/juejin-web/xitu_juejin_web/img/qr-code.4e391ff.png)

![](https://lf-web-assets.juejin.cn/obj/juejin-web/xitu_juejin_web/img/MaskGroup.13dfc4f.png)?雿??渲閎???舀??
頝唾?

銝?甇?
?喳??1銝芸?蝐?

![](https://lf-web-assets.juejin.cn/obj/juejin-web/xitu_juejin_web/8867e249c23a7c0ea596c139befc04d7.svg)

皜拚成?內

敶???憭梯揖嚗????殷??舐?餌霂?
???唾????
