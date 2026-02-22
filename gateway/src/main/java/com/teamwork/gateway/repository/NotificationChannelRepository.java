package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 通知通道資料存取層 */
@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, String> {

    /** 依租戶查詢全部通道 */
    List<NotificationChannel> findByTenantId(String tenantId);

    /** 依租戶與啟用狀態查詢 */
    List<NotificationChannel> findByTenantIdAndEnabled(String tenantId, boolean enabled);
}
