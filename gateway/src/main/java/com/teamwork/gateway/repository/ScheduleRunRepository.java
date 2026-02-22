package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.ScheduleRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRunRepository extends JpaRepository<ScheduleRun, String> {

    List<ScheduleRun> findByScheduleIdOrderByCreatedAtDesc(String scheduleId);

    List<ScheduleRun> findByTenantIdAndStatus(String tenantId, String status);
}
