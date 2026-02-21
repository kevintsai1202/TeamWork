package com.teamwork.gateway.controller;

import com.teamwork.gateway.service.SseConnectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SseConnectionService sseConnectionService;

    @InjectMocks
    private SseController sseController;

    @Test
    void streamTaskEvents_ShouldReturnSseEmitter() throws Exception {
        // Arrange
        mockMvc = MockMvcBuilders.standaloneSetup(sseController).build();
        String taskId = "test-task-123";
        SseEmitter mockEmitter = new SseEmitter();

        when(sseConnectionService.subscribe(taskId)).thenReturn(mockEmitter);

        // Act & Assert
        mockMvc.perform(get("/api/tasks/{taskId}/stream", taskId)
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                // Verify that SseEmitter was requested (async dispatch)
                .andExpect(request().asyncStarted());

        verify(sseConnectionService).subscribe(taskId);
    }
}
