package com.thock.back.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.thock.back.market",
        "com.thock.back.global"
})
public class MarketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketServiceApplication.class, args);
    }

}
