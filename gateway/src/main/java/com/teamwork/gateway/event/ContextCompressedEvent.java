package com.teamwork.gateway.event;

import org.springframework.context.ApplicationEvent;

public class ContextCompressedEvent extends ApplicationEvent {

    private final String taskId;
    private final long beforeTokens;
    private final long afterTokens;
    private final long savedTokens;
    private final double savedRatio;

    public ContextCompressedEvent(
            Object source,
            String taskId,
            long beforeTokens,
            long afterTokens,
            long savedTokens,
            double savedRatio) {
        super(source);
        this.taskId = taskId;
        this.beforeTokens = beforeTokens;
        this.afterTokens = afterTokens;
        this.savedTokens = savedTokens;
        this.savedRatio = savedRatio;
    }

    public String getTaskId() {
        return taskId;
    }

    public long getBeforeTokens() {
        return beforeTokens;
    }

    public long getAfterTokens() {
        return afterTokens;
    }

    public long getSavedTokens() {
        return savedTokens;
    }

    public double getSavedRatio() {
        return savedRatio;
    }
}