package com.teamwork.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping(@RequestParam(required = false) boolean error) {
        if (error) {
            throw new IllegalArgumentException("這是一個測試用的主動拋出例外狀況，用以驗證 GlobalExceptionHandler！");
        }
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "Team Work Gateway 伺服器運作正常！",
                "threads", "Virtual Threads Enable (if handled by standard Tomcat config)"));
    }
}
