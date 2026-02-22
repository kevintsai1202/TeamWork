package com.teamwork.gateway.agent;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 暫存待使用者回覆的問題（MVP 先用記憶體實作）。
 */
@Component
public class PendingUserQuestionStore {

    private final Map<String, String> pendingQuestions = new ConcurrentHashMap<>();

    public void put(String taskId, String question) {
        pendingQuestions.put(taskId, question);
    }

    public String get(String taskId) {
        return pendingQuestions.get(taskId);
    }

    public String remove(String taskId) {
        return pendingQuestions.remove(taskId);
    }

    public Set<String> taskIds() {
        return Set.copyOf(pendingQuestions.keySet());
    }

    public int size() {
        return pendingQuestions.size();
    }
}
