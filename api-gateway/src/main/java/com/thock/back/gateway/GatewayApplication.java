package com.thock.back.gateway;

import com.thock.back.global.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(
        scanBasePackages = {
                "com.thock.back.gateway"
        },
        exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
        }
)
@ComponentScan(
        basePackages = {
                "com.thock.back.gateway",
                "com.thock.back.global.security"
        },
        excludeFilters = {
                // GlobalConfig은 Gateway에서 사용하지 않으므로 제외
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.thock\\.back\\.global\\.config\\..*"
                ),
                // SecurityConfig, JwtAuthenticationFilter, InternalServiceAuthFilter, AuthUserArgumentResolver는 Gateway에서 사용하지 않으므로 제외
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.thock\\.back\\.global\\.security\\.(SecurityConfig|JwtAuthenticationFilter|InternalServiceAuthFilter|AuthUserArgumentResolver)"
                )
        }
)
@EnableConfigurationProperties(JwtProperties.class)
public class GatewayApplication {

    public static void main(String[] args) {
        // Gateway remains stateless, so DB/JPA auto-configuration stays disabled here.
        SpringApplication.run(GatewayApplication.class, args);
    }
}
