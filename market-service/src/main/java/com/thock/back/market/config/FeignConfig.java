package com.thock.back.market.config;

import feign.RequestInterceptor;
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

    // 내부 서비스 인증에 사용할 시크릿 키
    @Value("${SECURITY_SERVICE_INTERNAL_SECRET:${SECURITY_GATEWAY_INTERNAL_SECRET:}}")
    private String internalSecret;

    @Bean
    public Retryer retryer() {
        // period, maxPeriod, maxAttempts
        return new Retryer.Default(periodMs, maxPeriodMs, maxAttempts);
    }

    // Feign 클라이언트가 내부 서비스로 요청할 때, X-Internal-Auth 헤더에 시크릿 키를 자동으로 추가하는 인터셉터
    @Bean
    public RequestInterceptor internalAuthRequestInterceptor() {
        return template -> {
            if (internalSecret != null && !internalSecret.isBlank()) {
                template.header("X-Internal-Auth", internalSecret);
            }
        };
    }
}