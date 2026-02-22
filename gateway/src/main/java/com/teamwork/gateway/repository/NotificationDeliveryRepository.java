package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 通知派送紀錄資料存取層 */
@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, String> {

    /** 依 runId 查詢派送紀錄 */
    List<NotificationDelivery> findByRunId(String runId);

    /** 依 tenantId 查詢（含分頁排序可用 Pageable，此處先簡化） */
    List<NotificationDelivery> findByTenantId(String tenantId);

    /**
     * 去重查詢：同一 sourceRefId + eventType + channelId 是否已存在 SENT 或 PENDING 紀錄。
     * 用于避免重複派送。
     */
    Optional<NotificationDelivery> findBySourceRefIdAndEventTypeAndChannelIdAndStatusIn(
            String sourceRefId, String eventType, String channelId, List<String> statuses);

    /**
     * 查詢需要重試的失敗派送：狀態為 FAILED 且下次重試時間已到。
     */
    List<NotificationDelivery> findByStatusAndNextRetryAtBefore(String status, LocalDateTime now);

    /** 依通道查詢派送紀錄 */
    List<NotificationDelivery> findByChannelId(String channelId);
}
