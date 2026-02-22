package com.teamwork.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 設定：提供 ObjectMapper Bean，供通知模組等服務注入使用。
 */
@Configuration
public class JacksonConfig {

    /**
     * 建立預設 ObjectMapper。
     *
     * @return ObjectMapper 實例
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
