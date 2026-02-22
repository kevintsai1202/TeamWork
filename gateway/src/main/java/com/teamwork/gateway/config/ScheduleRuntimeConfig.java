package com.teamwork.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 排程 runtime 配置：提供專用 TaskScheduler 執行動態排程。
 */
@Configuration
public class ScheduleRuntimeConfig {

    /**
     * 建立專用排程執行緒池，供多排程並行觸發使用。
     */
    @Bean
    public TaskScheduler scheduleTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("schedule-runtime-");
        scheduler.setPoolSize(8);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }
}
