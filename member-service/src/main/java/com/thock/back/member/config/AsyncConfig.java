package com.thock.back.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync // 비동기 처리 활성화
@Configuration
public class AsyncConfig {
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 항상 유지되는 최소 스레드 수
        executor.setMaxPoolSize(5); // 필요하면 늘어날 수 있는 최대 스레드 수
        executor.setQueueCapacity(100); // 대기 작업 큐 크기
        executor.setThreadNamePrefix("async-"); // 디버깅을 위한 스레드 이름 접두사
        executor.initialize(); // 초기화
        return executor;
    }
}
