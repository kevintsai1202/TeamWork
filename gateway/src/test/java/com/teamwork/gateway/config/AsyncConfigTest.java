package com.teamwork.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void getAsyncExecutor_ShouldReturnConfiguredThreadPool() {
        // Act
        Executor executor = asyncConfig.getAsyncExecutor();

        // Assert
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor threadPool = (ThreadPoolTaskExecutor) executor;
        assertThat(threadPool.getCorePoolSize()).isEqualTo(5);
        assertThat(threadPool.getMaxPoolSize()).isEqualTo(20);
        assertThat(threadPool.getThreadNamePrefix()).isEqualTo("MasterAgent-Worker-");
    }

    @Test
    void getAsyncUncaughtExceptionHandler_ShouldReturnCustomHandler() throws NoSuchMethodException {
        // Act
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();

        // Assert
        assertThat(handler).isInstanceOf(AsyncConfig.CustomAsyncExceptionHandler.class);

        // Test the inner handler behavior (just ensure it doesn't throw when logging)
        Method dummyMethod = this.getClass().getDeclaredMethod("dummyMethod");
        handler.handleUncaughtException(new RuntimeException("Test Exception"), dummyMethod, "param1");
    }

    public void dummyMethod() {
        // Used for reflection retrieval
    }
}
