package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountToolPermissionRepository extends JpaRepository<AccountToolPermission, Long> {
    List<AccountToolPermission> findByUserId(String userId);
}
