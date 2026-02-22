package com.teamwork.gateway.agent;

import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;

/**
 * 以 AI 語意方式對 sub-agent 進行打分。
 */
public interface SubAgentSemanticScorer {

    /**
     * 回傳 key=name, value=score(0~1)。
     */
    Map<String, Double> score(String userInput, List<SubAgentDescriptor> descriptors, ChatModel chatModel);
}
