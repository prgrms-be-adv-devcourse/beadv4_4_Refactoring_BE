package com.thock.back.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ThockBackendApiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThockBackendApiServerApplication.class, args);
    }

}
