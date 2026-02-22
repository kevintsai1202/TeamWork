package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.TaskTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskTriggerRepository extends JpaRepository<TaskTrigger, String> {

    List<TaskTrigger> findByTenantIdAndEnabled(String tenantId, boolean enabled);

    Optional<TaskTrigger> findByWebhookKey(String webhookKey);
}
