package com.thock.back.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsGlobalConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 허용할 Origin
        corsConfig.setAllowedOrigins(Arrays.asList(
                "http://localhost:8080",  // API Gateway & Swagger UI
                "http://localhost:3000",  // Frontend Dev Server
                "http://localhost:3001"   // Frontend Alt Port
        ));

        // 허용할 HTTP 메서드
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        // 허용할 헤더
        corsConfig.addAllowedHeader("*");

        // 노출할 헤더
        corsConfig.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // 인증 정보 허용
        corsConfig.setAllowCredentials(true);

        // Preflight 캐시 시간
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}