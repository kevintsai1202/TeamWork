package com.teamwork.gateway.agent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Hybrid 路由可調參數。
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.routing.hybrid")
public class HybridRoutingProperties {

    private double aiWeight = 0.6;

    private double keywordWeight = 0.4;

    private double threshold = 0.55;
}
