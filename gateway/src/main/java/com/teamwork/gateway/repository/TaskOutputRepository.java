package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskOutputRepository extends JpaRepository<TaskOutput, String> {
    List<TaskOutput> findByTaskId(String taskId);
}
