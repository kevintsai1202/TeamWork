package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.TaskSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskScheduleRepository extends JpaRepository<TaskSchedule, String> {

    List<TaskSchedule> findByTenantId(String tenantId);

    List<TaskSchedule> findByTenantIdAndEnabled(String tenantId, boolean enabled);

    List<TaskSchedule> findByEnabled(boolean enabled);
}
