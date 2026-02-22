package com.teamwork.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamwork.gateway.dto.NotificationPolicyRequest;
import com.teamwork.gateway.dto.NotificationPolicyResponse;
import com.teamwork.gateway.entity.NotificationPolicy;
import com.teamwork.gateway.repository.NotificationPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotificationPolicyServiceTest — 驗證通知策略 CRUD 與 channelIds JSON 轉換。
 */
@ExtendWith(MockitoExtension.class)
class NotificationPolicyServiceTest {

    @Mock
    private NotificationPolicyRepository policyRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationPolicyService service;

    /** 建立策略 — 正常情境，應儲存並回傳帶 channelIds 的 response */
    @Test
    void createPolicy_shouldSaveAndReturnResponse() {
        NotificationPolicyRequest req = new NotificationPolicyRequest();
        req.setName("default-policy");
        req.setOnSuccess(true);
        req.setOnFailed(true);
        req.setChannelIds(List.of("ch-1", "ch-2"));
        req.setTenantId("t1");

        NotificationPolicy saved = new NotificationPolicy();
        saved.setId("pol-1");
        saved.setName("default-policy");
        saved.setTenantId("t1");
        saved.setOnSuccess(true);
        saved.setOnFailed(true);
        saved.setChannelIdsJson("[\"ch-1\",\"ch-2\"]");

        when(policyRepository.save(any())).thenReturn(saved);

        NotificationPolicyResponse resp = service.createPolicy(req);

        assertThat(resp.getId()).isEqualTo("pol-1");
        assertThat(resp.getChannelIds()).containsExactlyInAnyOrder("ch-1", "ch-2");
        verify(policyRepository).save(any(NotificationPolicy.class));
    }

    /** 更新策略 — 找不到 ID 應拋出例外 */
    @Test
    void updatePolicy_notFound_shouldThrow() {
        when(policyRepository.findById("not-exist")).thenReturn(Optional.empty());

        NotificationPolicyRequest req = new NotificationPolicyRequest();
        req.setName("x");

        assertThatThrownBy(() -> service.updatePolicy("not-exist", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 刪除策略 — 應呼叫 deleteById */
    @Test
    void deletePolicy_shouldCallDeleteById() {
        NotificationPolicy policy = new NotificationPolicy();
        policy.setId("pol-2");
        policy.setTenantId("t1");

        when(policyRepository.findById("pol-2")).thenReturn(Optional.of(policy));

        service.deletePolicy("pol-2");

        verify(policyRepository).delete(policy);
    }

    /** listPolicies — 應呼叫 findByTenantId */
    @Test
    void listPolicies_shouldCallFindByTenantId() {
        when(policyRepository.findByTenantId("t1")).thenReturn(List.of());

        var result = service.listPolicies("t1");

        assertThat(result.getPolicies()).isEmpty();
        verify(policyRepository).findByTenantId("t1");
    }

    /** findById — 找不到應回傳 empty Optional */
    @Test
    void findById_notFound_shouldReturnEmpty() {
        when(policyRepository.findById("p99")).thenReturn(Optional.empty());

        var result = service.findById("p99");

        assertThat(result).isEmpty();
    }
}
