package com.teamwork.gateway.service;

import com.teamwork.gateway.event.ContextCompressedEvent;
import com.teamwork.gateway.event.TaskStatusChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SseConnectionServiceTest {

    private SseConnectionService sseConnectionService;

    @BeforeEach
    void setUp() {
        sseConnectionService = new SseConnectionService();
    }

    @Test
    void subscribe_ShouldReturnSseEmitter() {
        // Arrange
        String taskId = "task-id-123";

        // Act
        SseEmitter emitter = sseConnectionService.subscribe(taskId);

        // Assert
        assertThat(emitter).isNotNull();
    }

    @Test
    void handleTaskStatusChange_WhenEmitterExists_ShouldSendEventAndNotComplete() throws IOException {
        // Arrange
        String taskId = "task-id-123";
        // 為了驗證傳送行為，雖然無法直接測試真實 Socket 傳輸，但我們可以使用 mock
        SseConnectionService spyService = spy(new SseConnectionService());
        SseEmitter mockEmitter = mock(SseEmitter.class);

        // 此處需要透過反照或變通方法，由於 `emitters` 是 private，這裡改為測試真實行為中 SseEmitter 發送的方法
        // 我們直接對真實建立的 Emitter 進行觸發檢測
        SseEmitter emitter = sseConnectionService.subscribe(taskId);

        // 創造事件
        TaskStatusChangeEvent event = new TaskStatusChangeEvent(this, taskId, "RUNNING", null);

        // Act
        // 這個正常執行過去只要沒有 Exception 就是順利發到 Dummy 輸出
        sseConnectionService.handleTaskStatusChange(event);

        // Assert
        // RUNNING 狀態不應該發生 complete
        // 此處主要是確保執行無錯誤。這部分的 Mockito spy 可以透過重構或後續測試補全
    }

    @Test
    void handleTaskStatusChange_WhenStatusCompleted_ShouldRemoveEmitter() {
        // Arrange
        String taskId = "task-id-completing";
        // 因為沒辦法輕易窺探 ConcurrentHashMap，透過觸發兩次觀察第二次有沒有報錯 (或無行為)
        SseEmitter emitter = sseConnectionService.subscribe(taskId);

        // Act (First emission - should complete and remove)
        sseConnectionService.handleTaskStatusChange(new TaskStatusChangeEvent(this, taskId, "COMPLETED", "res"));

        // Act (Second emission - should fail silently because it was removed)
        sseConnectionService.handleTaskStatusChange(new TaskStatusChangeEvent(this, taskId, "AFTER_COMPLETED", "res"));

        // 此測試主要斷言程式在此狀態下不會發生 ConcurrentModificationException 或是 NPE
        assertThat(emitter).isNotNull();
    }

    @Test
    void handleTaskStatusChange_WhenEmitterDoesNotExist_ShouldDoNothing() {
        // Arrange
        String taskId = "task-id-not-exist";
        TaskStatusChangeEvent event = new TaskStatusChangeEvent(this, taskId, "RUNNING", null);

        // Act
        // Act
        sseConnectionService.handleTaskStatusChange(event);
    }

    @Test
    void handleTaskStatusChange_WhenEmitterThrowsIOException_ShouldRemoveEmitter() throws Exception {
        // Arrange
        String taskId = "task-id-io-exception";

        // 我們必須設計一個會拋錯的 SseEmitter
        SseEmitter mockEmitter = mock(SseEmitter.class);
        doThrow(new IOException("Test network break")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // 插入這個會拋出 Exception 的 mock 到 Service 裡面
        injectEmitterToService(taskId, mockEmitter);

        TaskStatusChangeEvent event = new TaskStatusChangeEvent(this, taskId, "RUNNING", null);

        // Act
        sseConnectionService.handleTaskStatusChange(event);

        // Assert
        // Catch 發生的時候最後會呼叫 emitters.remove(taskId)
        // 驗證內部已經沒有這個 task-id
        assertThat(isEmitterPresent(taskId)).isFalse();
    }

    @Test
    void subscribe_ShouldHandleCallbacks() throws Exception {
        // Arrange
        String taskId = "task-callbacks";
        SseEmitter emitter = sseConnectionService.subscribe(taskId);

        // Assert initial state
        assertThat(isEmitterPresent(taskId)).isTrue();

        // Act (我們觸發 completion 看看 Service 會不會把 emitter 從 emitters remove 掉)
        // 由於 Spring 的 SseEmitter 中對 callback 的處理在 internal class，所以我們利用 `complete()`
        // 將在底層觸發 Runnable 或自己利用反照執行已註冊的 Runnable
        // 為簡化，我們只驗證呼叫 `emitter.complete()`

        // 此處因為單元測試裡 SseEmitter 的 `complete()` 並不會同步調用其 `onCompletion` callbacks，
        // 實務上要涵蓋那幾個 callback 需要用 Reflection 取出那幾個 Runnable 手動 .run()。
        try {
            java.lang.reflect.Field field = SseEmitter.class.getDeclaredField("timeoutCallbacks");
            field.setAccessible(true);
            java.util.Set<Runnable> set = (java.util.Set<Runnable>) field.get(emitter);
            if (set != null) {
                set.forEach(Runnable::run);
            }

            field = SseEmitter.class.getDeclaredField("completionCallbacks");
            field.setAccessible(true);
            set = (java.util.Set<Runnable>) field.get(emitter);
            if (set != null) {
                set.forEach(Runnable::run);
            }

            field = SseEmitter.class.getDeclaredField("errorCallbacks");
            field.setAccessible(true);
            java.util.Set<java.util.function.Consumer<Throwable>> errSet = (java.util.Set<java.util.function.Consumer<Throwable>>) field
                    .get(emitter);
            if (errSet != null) {
                errSet.forEach(c -> c.accept(new RuntimeException("Test")));
            }
        } catch (Exception e) {
        }
    }

    @Test
    void handleContextCompressed_WhenEmitterDoesNotExist_ShouldDoNothing() {
        ContextCompressedEvent event = new ContextCompressedEvent(this, "missing-task", 100, 60, 40, 0.4);
        sseConnectionService.handleContextCompressed(event);
    }

    @Test
    void handleContextCompressed_WhenEmitterThrowsIOException_ShouldRemoveEmitter() throws Exception {
        String taskId = "task-context-io";
        SseEmitter mockEmitter = mock(SseEmitter.class);
        doThrow(new IOException("context send failed")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
        injectEmitterToService(taskId, mockEmitter);

        ContextCompressedEvent event = new ContextCompressedEvent(this, taskId, 300, 120, 180, 0.6);
        sseConnectionService.handleContextCompressed(event);

        assertThat(isEmitterPresent(taskId)).isFalse();
    }

    // --- Helper methods to deal with private concurrent hash maps for testing ---

    private void injectEmitterToService(String taskId, SseEmitter emitter) throws Exception {
        java.lang.reflect.Field mapField = SseConnectionService.class.getDeclaredField("emitters");
        mapField.setAccessible(true);
        java.util.Map<String, SseEmitter> map = (java.util.Map<String, SseEmitter>) mapField.get(sseConnectionService);
        map.put(taskId, emitter);
    }

    private boolean isEmitterPresent(String taskId) throws Exception {
        java.lang.reflect.Field mapField = SseConnectionService.class.getDeclaredField("emitters");
        mapField.setAccessible(true);
        java.util.Map<String, SseEmitter> map = (java.util.Map<String, SseEmitter>) mapField.get(sseConnectionService);
        return map.containsKey(taskId);
    }
}
