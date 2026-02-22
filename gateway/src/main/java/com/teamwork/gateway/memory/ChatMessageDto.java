package com.teamwork.gateway.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Map;

/**
 * Data Transfer Object for storing Spring AI Messages in Redis.
 * Avoids Jackson polymorphic deserialization issues.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String type; // SYSTEM, USER, ASSISTANT
    private String content;
    private Map<String, Object> metadata;

    public static ChatMessageDto fromMessage(Message message) {
        String type;
        if (message instanceof SystemMessage)
            type = "SYSTEM";
        else if (message instanceof UserMessage)
            type = "USER";
        else if (message instanceof AssistantMessage)
            type = "ASSISTANT";
        else
            type = "UNKNOWN";

        return ChatMessageDto.builder()
                .type(type)
                .content(message.getText())
                .metadata(message.getMetadata())
                .build();
    }

    public Message toMessage() {
        return switch (this.type) {
            case "SYSTEM" -> new SystemMessage(this.content);
            case "USER" -> new UserMessage(this.content);
            case "ASSISTANT" -> new AssistantMessage(this.content);
            default -> new UserMessage(this.content); // Fallback
        };
    }
}
