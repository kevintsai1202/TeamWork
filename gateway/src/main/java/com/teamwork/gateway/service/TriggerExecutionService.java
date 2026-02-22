package com.teamwork.gateway.service;

import com.teamwork.gateway.agent.MasterAgent;
import com.teamwork.gateway.dto.WebhookTriggerRequest;
import com.teamwork.gateway.dto.WebhookTriggerResponse;
import com.teamwork.gateway.entity.TaskRecord;
import com.teamwork.gateway.entity.TaskTrigger;
import com.teamwork.gateway.repository.AgentProfileRepository;
import com.teamwork.gateway.repository.TaskRecordRepository;
import com.teamwork.gateway.repository.TaskTriggerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TriggerExecutionService {

    private static final String TRIGGER_SOURCE_WEBHOOK = "WEBHOOK";
    private static final String TARGET_TYPE_AGENT = "AGENT";

    private final TaskTriggerRepository taskTriggerRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final MasterAgent masterAgent;
    private final WebhookSecurityService webhookSecurityService;
    /** 通知派送服務，可選注入（避免既有測試破壞）*/
    @Nullable
    private final NotificationDispatchService notificationDispatchService;

    @Transactional
        public WebhookTriggerResponse executeWebhook(
            String webhookKey,
            WebhookTriggerRequest request,
            String triggerTimestamp,
            String triggerNonce,
            String triggerSignature) {
        TaskTrigger trigger = taskTriggerRepository.findByWebhookKey(webhookKey)
                .orElseThrow(() -> new IllegalArgumentException("Webhook trigger not found: " + webhookKey));

        if (!trigger.isEnabled()) {
            throw new IllegalArgumentException("Trigger is disabled: " + webhookKey);
        }

        String triggerSource = normalize(trigger.getTriggerSource());
        if (!TRIGGER_SOURCE_WEBHOOK.equals(triggerSource)) {
            throw new IllegalArgumentException("Trigger source is not WEBHOOK: " + trigger.getTriggerSource());
        }

        String targetType = normalize(trigger.getTargetType());
        if (!TARGET_TYPE_AGENT.equals(targetType)) {
            throw new IllegalArgumentException("Currently only AGENT targetType is supported for webhook trigger");
        }

        String profileId = trigger.getTargetRefId();
        if (profileId == null || profileId.isBlank() || !agentProfileRepository.existsById(profileId)) {
            throw new IllegalArgumentException("Agent profile not found: " + profileId);
        }

        if (request == null || request.getInputPayload() == null || request.getInputPayload().isBlank()) {
            throw new IllegalArgumentException("inputPayload is required");
        }

        webhookSecurityService.validate(
            trigger,
            webhookKey,
            request,
            triggerTimestamp,
            triggerNonce,
            triggerSignature);

        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setProfileId(profileId);
        taskRecord.setParentTaskId(null);
        taskRecord.setInputPayload(request.getInputPayload());
        taskRecord.setStatus("PENDING");

        TaskRecord saved = taskRecordRepository.save(taskRecord);
        masterAgent.processTask(saved.getId(), saved.getInputPayload());

        // 若觸發器設定了通知策略，則派送通知
        if (notificationDispatchService != null && trigger.getNotificationPolicyId() != null) {
            notificationDispatchService.dispatchForTrigger(
                    saved.getId(),
                    trigger.getId(),
                    trigger.getTenantId(),
                    trigger.getNotificationPolicyId(),
                    "SUCCESS",
                    null);
        }

        return new WebhookTriggerResponse(TRIGGER_SOURCE_WEBHOOK, saved.getId(), saved.getStatus());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
