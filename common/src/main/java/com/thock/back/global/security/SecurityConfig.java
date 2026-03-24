package com.thock.back.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 내부 API 인증 필터 특정 경로에만 적용하기 위해 필터 체인에서 직접 등록하지 않고, InternalServiceAuthFilter의 shouldNotFilter() 메서드에서 "/internal/"이 포함된 URI만 필터링하도록 구현
    private final InternalServiceAuthFilter internalServiceAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // H2 콘솔용 (개발)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                // 세션 사용 안 함 (JWT 기반)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 모든 요청 허용 (Gateway가 이미 검증)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

                // 기본 폼 로그인/베이직 인증 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 내부 서비스 인증 필터 등록 ("/internal/"이 포함된 URI만 필터링)
                .addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class);
                // JWT 필터 제거
                //.addFilterBefore(jwtAuthenticationFilter, ...);

        return http.build();
    }
}

