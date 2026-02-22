package com.teamwork.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:15432/teamwork",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.ai.openai.api-key=test-key"
})
class TeamworkGatewayApplicationTests {

    @Test
    void contextLoads() {
        // This test simply verifies if the Spring application context starts
        // successfully.
    }
}
