package com.teamwork.gateway.controller;

import com.teamwork.gateway.service.SseConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class SseController {

    private final SseConnectionService sseConnectionService;

    /**
     * 提供給前端客戶端訂閱特定 Task 的非同步進度
     *
     * @param taskId 任務的 UUID
     * @return 保持連線的 SseEmitter
     */
    @GetMapping(path = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTaskEvents(@PathVariable("taskId") String taskId) {
        return sseConnectionService.subscribe(taskId);
    }
}
