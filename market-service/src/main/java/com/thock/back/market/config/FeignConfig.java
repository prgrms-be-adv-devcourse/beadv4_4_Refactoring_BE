package com.thock.back.market.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Retryer retryer() {
        // period, maxPeriod, maxAttempts
        return new Retryer.Default(100, 300, 3);
    }
}
