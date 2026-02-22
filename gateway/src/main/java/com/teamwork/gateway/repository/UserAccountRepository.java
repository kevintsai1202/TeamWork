package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    boolean existsByIdAndStatus(String id, String status);

    Optional<UserAccount> findByTenantIdAndUsername(String tenantId, String username);
}