package com.teamwork.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 啟用非同步執行，為未來的 Agent 非同步迴圈做準備
public class TeamworkGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamworkGatewayApplication.class, args);
    }
}
