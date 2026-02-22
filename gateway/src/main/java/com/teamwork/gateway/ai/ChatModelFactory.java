package com.teamwork.gateway.ai;

import com.teamwork.gateway.entity.AiModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class ChatModelFactory {

    public ChatModel createChatModel(AiModel aiModel) {
        if (!aiModel.isActive()) {
            throw new IllegalArgumentException("AI Model " + aiModel.getName() + " is currently inactive.");
        }

        return switch (aiModel.getProvider().toUpperCase()) {
            case "OPENAI" -> createOpenAiModel(aiModel);
            // Future providers like ANTHROPIC, OLLAMA can be added here
            default -> throw new UnsupportedOperationException("Unsupported AI Provider: " + aiModel.getProvider());
        };
    }

    private ChatModel createOpenAiModel(AiModel aiModel) {
        // Fallback to OpenAI's default API URL if not specified
        String baseUrl = (aiModel.getEndpointUrl() != null && !aiModel.getEndpointUrl().trim().isEmpty())
                ? aiModel.getEndpointUrl()
                : "https://api.openai.com";

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(aiModel.getApiKey())
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(aiModel.getName())
                // .temperature(0.7) // Default or DB-driven
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
