package com.thock.back.market.config;

import feign.Target;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

@Configuration
public class FeignCircuitBreakerConfig {

    @Bean
    public CircuitBreakerNameResolver circuitBreakerNameResolver() {
        // CB 이름을 메서드명이 아니라 @FeignClient(name=...) 으로 고정
        return (String feignClientName, Target<?> target, Method method) -> feignClientName;
    }
}
