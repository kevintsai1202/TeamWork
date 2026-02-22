package com.teamwork.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "agent_profiles")
public class AgentProfile {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String name;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "default_model_id")
    private String defaultModelId;

    /** 是否啟用沙盒執行模式，啟用時會路由至 SandboxExecutionProvider */
    @Column(name = "sandbox_enabled")
    private boolean sandboxEnabled = false;

    /**
     * 沙盒類型：
     * <ul>
     *   <li>NONE — 不啟用沙盒（預設）</li>
     *   <li>LOCAL — 直接在主機執行（無容器隔離，快速開發用）</li>
     *   <li>DOCKER — Docker 容器隔離（透過 agent-sandbox-docker / Testcontainers）</li>
     *   <li>E2B — 遠端 Firecracker microVM（未來版本，需 E2B_API_KEY）</li>
     * </ul>
     */
    @Column(name = "sandbox_type", length = 20)
    private String sandboxType = "NONE";

    /**
     * Docker 沙盒所用的映像名稱；null 時採用 application.yml sandbox.docker.default-image 全域預設。
     * sandboxType 非 DOCKER 時忽略此欄位。
     */
    @Column(name = "docker_image", length = 200)
    private String dockerImage;

}
