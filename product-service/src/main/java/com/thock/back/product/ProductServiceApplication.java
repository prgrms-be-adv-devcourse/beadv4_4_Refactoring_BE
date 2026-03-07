package com.thock.back.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.thock.back.product",
        "com.thock.back.global"
})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {
        "com.thock.back.product",
        "com.thock.back.global.inbox.repository"
})
@EntityScan(basePackages = {
        "com.thock.back.product",
        "com.thock.back.global.inbox.entity"
})
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

}
