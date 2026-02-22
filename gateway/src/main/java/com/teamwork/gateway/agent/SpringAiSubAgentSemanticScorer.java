package com.teamwork.gateway.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用 Spring AI 對 sub-agent 進行語意打分。
 */
@Component
@Slf4j
public class SpringAiSubAgentSemanticScorer implements SubAgentSemanticScorer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Double> score(String userInput, List<SubAgentDescriptor> descriptors, ChatModel chatModel) {
        if (chatModel == null || descriptors == null || descriptors.isEmpty()) {
            return Map.of();
        }

        try {
            ChatClient chatClient = ChatClient.create(chatModel);
            String descriptorJson = objectMapper.writeValueAsString(descriptors);
            String result = chatClient.prompt()
                    .system("""
                            你是路由評分器。請根據 userInput 與每個 sub-agent 的 name/description/tools，
                            回傳 JSON 物件，key 是 sub-agent 名稱，value 是 0~1 分數。
                            僅輸出 JSON，不要額外文字。
                            """)
                    .user("userInput=" + userInput + "\nsubAgents=" + descriptorJson)
                    .call()
                    .content();

            Map<String, Object> parsed = objectMapper.readValue(result, new TypeReference<>() {
            });
            Map<String, Double> scores = new HashMap<>();
            for (SubAgentDescriptor descriptor : descriptors) {
                Object raw = parsed.get(descriptor.name());
                if (raw instanceof Number number) {
                    scores.put(descriptor.name(), clamp(number.doubleValue()));
                }
            }
            return scores;
        } catch (Exception ex) {
            log.warn("Semantic routing score failed, fallback to keyword-only routing", ex);
            return Map.of();
        }
    }

    private double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }
}
