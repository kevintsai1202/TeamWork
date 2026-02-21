package com.teamwork.gateway.event;

import org.springframework.context.ApplicationEvent;

public class TaskStatusChangeEvent extends ApplicationEvent {

    private final String taskId;
    private final String status;
    private final String payload;

    public TaskStatusChangeEvent(Object source, String taskId, String status, String payload) {
        super(source);
        this.taskId = taskId;
        this.status = status;
        this.payload = payload;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }
}
