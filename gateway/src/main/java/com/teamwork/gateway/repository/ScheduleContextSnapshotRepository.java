package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.ScheduleContextSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleContextSnapshotRepository extends JpaRepository<ScheduleContextSnapshot, String> {

    List<ScheduleContextSnapshot> findByScheduleIdOrderByCreatedAtDesc(String scheduleId);

    List<ScheduleContextSnapshot> findByRunId(String runId);

    Optional<ScheduleContextSnapshot> findTopByScheduleIdAndContextSegmentKeyOrderByCreatedAtDesc(
            String scheduleId,
            String contextSegmentKey);

        List<ScheduleContextSnapshot> findByScheduleIdAndContextSegmentKeyOrderByCreatedAtDesc(
            String scheduleId,
            String contextSegmentKey);
}
