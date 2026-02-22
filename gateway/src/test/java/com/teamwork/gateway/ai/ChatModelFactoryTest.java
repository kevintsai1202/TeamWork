package com.teamwork.gateway.ai;

import com.teamwork.gateway.entity.AiModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatModelFactoryTest {

    private final ChatModelFactory chatModelFactory = new ChatModelFactory();

    @Test
    void createChatModel_ShouldThrowWhenModelInactive() {
        AiModel model = new AiModel();
        model.setName("gpt-4o-mini");
        model.setProvider("OPENAI");
        model.setActive(false);

        assertThatThrownBy(() -> chatModelFactory.createChatModel(model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currently inactive");
    }

    @Test
    void createChatModel_ShouldThrowWhenProviderUnsupported() {
        AiModel model = new AiModel();
        model.setName("test-model");
        model.setProvider("UNKNOWN");
        model.setApiKey("test-key");
        model.setActive(true);

        assertThatThrownBy(() -> chatModelFactory.createChatModel(model))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unsupported AI Provider");
    }

    @Test
    void createChatModel_ShouldCreateOpenAiModelWithCustomEndpoint() {
        AiModel model = new AiModel();
        model.setName("gpt-4o-mini");
        model.setProvider("OPENAI");
        model.setApiKey("test-key");
        model.setEndpointUrl("https://example.openai.local");
        model.setActive(true);

        ChatModel chatModel = chatModelFactory.createChatModel(model);

        assertThat(chatModel).isNotNull();
    }

    @Test
    void createChatModel_ShouldCreateOpenAiModelWithDefaultEndpointWhenEmpty() {
        AiModel model = new AiModel();
        model.setName("gpt-4o-mini");
        model.setProvider("openai");
        model.setApiKey("test-key");
        model.setEndpointUrl("   ");
        model.setActive(true);

        ChatModel chatModel = chatModelFactory.createChatModel(model);

        assertThat(chatModel).isNotNull();
    }
}
