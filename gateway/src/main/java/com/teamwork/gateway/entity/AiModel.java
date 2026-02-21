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
@Table(name = "ai_models")
public class AiModel {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String apiKey;

    @Column(name = "endpoint_url")
    private String endpointUrl;

    @Column(nullable = false)
    private boolean active = true;

}
