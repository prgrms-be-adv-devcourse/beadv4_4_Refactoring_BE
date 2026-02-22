package com.thock.back.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.thock.back.market",
        "com.thock.back.global"
})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {
        "com.thock.back.market",
        "com.thock.back.global.outbox.repository"
})
@EntityScan(basePackages = {
        "com.thock.back.market",
        "com.thock.back.global.outbox.entity"
})
@EnableScheduling
public class MarketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketServiceApplication.class, args);
    }

}
