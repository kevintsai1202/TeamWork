package com.teamwork.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gateway.identity")
public class IdentityPolicyProperties {

    /**
     * 是否允許從 request body 讀取 userId。
     * dev/test 預設開啟；prod 應關閉並改由 Token claims 注入。
     */
    private boolean allowRequestUserId = true;
}