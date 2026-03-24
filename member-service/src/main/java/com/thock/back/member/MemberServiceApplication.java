package com.thock.back.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.thock.back.member",
        "com.thock.back.global"
}
)
@EnableJpaAuditing
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "com.thock.back.member",
})
@EntityScan(basePackages = {
        "com.thock.back.member",
})
public class MemberServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberServiceApplication.class, args);
    }

}
