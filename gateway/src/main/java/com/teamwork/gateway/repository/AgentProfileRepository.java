package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentProfileRepository extends JpaRepository<AgentProfile, String> {
}
