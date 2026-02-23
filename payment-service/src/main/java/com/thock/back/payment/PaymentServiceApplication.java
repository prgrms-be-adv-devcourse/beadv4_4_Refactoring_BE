package com.thock.back.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = {
        "com.thock.back.payment",
        "com.thock.back.global"
})
@EnableJpaAuditing
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "com.thock.back.payment",
        "com.thock.back.global.outbox.repository"
})
@EntityScan(basePackages = {
        "com.thock.back.payment",
        "com.thock.back.global.outbox.entity"
})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}
