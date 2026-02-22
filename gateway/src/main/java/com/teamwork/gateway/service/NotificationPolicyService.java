package com.teamwork.gateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamwork.gateway.dto.NotificationPolicyListResponse;
import com.teamwork.gateway.dto.NotificationPolicyRequest;
import com.teamwork.gateway.dto.NotificationPolicyResponse;
import com.teamwork.gateway.entity.NotificationPolicy;
import com.teamwork.gateway.repository.NotificationPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 通知策略管理服務：負責 CRUD 與通道綁定操作。
 * 策略決定哪些事件時機（啟動/成功/失敗/逾時）發送通知，以及發送到哪些通道。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationPolicyService {

    private final NotificationPolicyRepository policyRepository;
    private final ObjectMapper objectMapper;

    /**
     * 建立通知策略。
     *
     * @param request 建立請求
     * @return 策略回應 DTO
     */
    public NotificationPolicyResponse createPolicy(NotificationPolicyRequest request) {
        validateRequest(request);
        NotificationPolicy policy = new NotificationPolicy();
        applyRequest(policy, request);
        return toResponse(policyRepository.save(policy));
    }

    /**
     * 更新通知策略。
     *
     * @param policyId 策略 ID
     * @param request  更新請求
     * @return 更新後的策略回應 DTO
     */
    public NotificationPolicyResponse updatePolicy(String policyId, NotificationPolicyRequest request) {
        NotificationPolicy policy = getOrThrow(policyId);
        applyRequest(policy, request);
        return toResponse(policyRepository.save(policy));
    }

    /**
     * 刪除通知策略。
     *
     * @param policyId 策略 ID
     */
    public void deletePolicy(String policyId) {
        NotificationPolicy policy = getOrThrow(policyId);
        policyRepository.delete(policy);
    }

    /**
     * 查詢租戶下的通知策略列表。
     *
     * @param tenantId 租戶 ID
     * @return 策略列表回應 DTO
     */
    public NotificationPolicyListResponse listPolicies(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        List<NotificationPolicy> policies = policyRepository.findByTenantId(tenantId);
        return new NotificationPolicyListResponse(policies.stream().map(this::toResponse).toList());
    }

    /**
     * 依 ID 查詢單一策略。
     *
     * @param policyId 策略 ID
     * @return 策略回應 DTO
     */
    public NotificationPolicyResponse getPolicy(String policyId) {
        return toResponse(getOrThrow(policyId));
    }

    // ── 內部工具方法（供 NotificationDispatchService 使用）──────────────

    /**
     * 依 ID 載入策略實體（dispatch service 使用）。
     *
     * @param policyId 策略 ID
     * @return NotificationPolicy 實體（Optional 包裝）
     */
    public java.util.Optional<NotificationPolicy> findById(String policyId) {
        return policyRepository.findById(policyId);
    }

    /**
     * 解析策略中的通道 ID 清單。
     *
     * @param policy 策略實體
     * @return 通道 ID 清單（若解析失敗回空清單）
     */
    public List<String> parseChannelIds(NotificationPolicy policy) {
        if (policy.getChannelIdsJson() == null || policy.getChannelIdsJson().isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(policy.getChannelIdsJson(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse channelIdsJson for policy {}: {}", policy.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── 私有輔助方法 ─────────────────────────────────────────────

    private NotificationPolicy getOrThrow(String policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Notification policy not found: " + policyId));
    }

    /** 驗證建立請求必填欄位 */
    private void validateRequest(NotificationPolicyRequest request) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    /** 將請求欄位套用到實體 */
    private void applyRequest(NotificationPolicy policy, NotificationPolicyRequest request) {
        if (request.getTenantId() != null) policy.setTenantId(request.getTenantId());
        if (request.getName() != null) policy.setName(request.getName());
        if (request.getOnStarted() != null) policy.setOnStarted(request.getOnStarted());
        if (request.getOnSuccess() != null) policy.setOnSuccess(request.getOnSuccess());
        if (request.getOnFailed() != null) policy.setOnFailed(request.getOnFailed());
        if (request.getOnTimeout() != null) policy.setOnTimeout(request.getOnTimeout());
        if (request.getTemplateId() != null) policy.setTemplateId(request.getTemplateId());
        if (request.getChannelIds() != null) {
            try {
                policy.setChannelIdsJson(objectMapper.writeValueAsString(request.getChannelIds()));
            } catch (Exception e) {
                log.warn("Failed to serialize channelIds: {}", e.getMessage());
            }
        }
    }

    /** 將實體轉換為回應 DTO */
    private NotificationPolicyResponse toResponse(NotificationPolicy policy) {
        return new NotificationPolicyResponse(
                policy.getId(),
                policy.getTenantId(),
                policy.getName(),
                policy.isOnStarted(),
                policy.isOnSuccess(),
                policy.isOnFailed(),
                policy.isOnTimeout(),
                parseChannelIds(policy),
                policy.getTemplateId(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
