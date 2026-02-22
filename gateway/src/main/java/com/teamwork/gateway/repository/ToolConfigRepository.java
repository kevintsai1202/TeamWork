package com.teamwork.gateway.repository;

import com.teamwork.gateway.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ToolConfigRepository extends JpaRepository<ToolConfig, String> {
    List<ToolConfig> findByType(String type);

    Optional<ToolConfig> findByName(String name);

    long countByIdIn(Collection<String> ids);
}
