package com.lightcare.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务执行器（PR3 给 MealRecognitionExecutor 用）。
 *
 * Meal 识别是 IO+LLM bound（13-19s/单），不能用默认 ForkJoinPool（会拖慢 web 线程）。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mealRecExecutorPool")
    public ThreadPoolTaskExecutor mealRecExecutorPool() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(20);
        ex.setThreadNamePrefix("meal-rec-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        ex.initialize();
        return ex;
    }
}
