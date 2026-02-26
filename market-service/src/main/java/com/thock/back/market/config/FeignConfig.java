package com.thock.back.market.config;

import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Value("${market.feign.retry.period-ms:100}")
    private long periodMs;

    @Value("${market.feign.retry.max-period-ms:300}")
    private long maxPeriodMs;

    @Value("${market.feign.retry.max-attempts:3}")
    private int maxAttempts;

    @Bean
    public Retryer retryer() {
        // period, maxPeriod, maxAttempts
        return new Retryer.Default(periodMs, maxPeriodMs, maxAttempts);
    }
}
