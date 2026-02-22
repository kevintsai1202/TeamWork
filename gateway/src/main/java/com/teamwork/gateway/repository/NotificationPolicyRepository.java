package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.NotificationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 通知策略資料存取層 */
@Repository
public interface NotificationPolicyRepository extends JpaRepository<NotificationPolicy, String> {

    /** 依租戶查詢全部策略 */
    List<NotificationPolicy> findByTenantId(String tenantId);
}
