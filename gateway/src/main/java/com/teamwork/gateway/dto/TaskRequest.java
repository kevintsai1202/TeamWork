package com.teamwork.gateway.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class TaskRequest {
    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "profileId is required")
    private String profileId;

    private String parentTaskId;

    @NotBlank(message = "inputPayload is required")
    private String inputPayload;
}
