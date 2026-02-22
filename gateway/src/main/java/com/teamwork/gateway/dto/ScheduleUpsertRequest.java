package com.teamwork.gateway.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ScheduleUpsertRequest {
    private String tenantId;
    private String name;
    private Boolean enabled;
    private String scheduleType;
    private String cronExpr;
    private Integer intervalSeconds;
    private String timezone;
    private String targetType;
    private String targetRefId;
    private String payloadJson;
    private Integer priority;
    private Integer maxConcurrentRuns;
    private String contextMode;
    private Integer contextRetentionRuns;
    private Integer contextMaxTokens;
    private String notificationPolicyId;
}
