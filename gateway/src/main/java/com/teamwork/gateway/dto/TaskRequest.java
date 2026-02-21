package com.teamwork.gateway.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskRequest {
    private String profileId;
    private String parentTaskId;
    private String inputPayload;
}
