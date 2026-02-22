package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.ContextDeletionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextDeletionAuditRepository extends JpaRepository<ContextDeletionAudit, String> {
}
