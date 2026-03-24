package com.thock.back.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableRetry(order = Ordered.LOWEST_PRECEDENCE - 1)
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE)
public class RetryConfig {
}