package com.thock.back.api.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // H2 콘솔/Swagger는 CSRF가 걸리면 불편해서 개발용으로 끔
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // H2 console iframe

                // 세션 사용 안 함 (JWT 기반)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**",

                                // 인증/회원가입
                                "/api/v1/auth/**",
                                "/api/v1/members/signup",

                                // PG return pages
                                "/api/v1/payments/confirm/**",
                                "/success.html/**",
                                "/checkout.html/**",
                                "/fail.html/**",
                                // 장바구니, 상품
                                "/api/v1/carts/**",
                                "/api/v1/orders/**",

                                // 내부 API
                                "/api/v1/products/internal/**",
                                "/api/v1/payments/internal/**"
                        ).permitAll()
                        .anyRequest().authenticated()   // ← 여기 중요 (JWT 없으면 접근 불가)
                )

                // 기본 폼 로그인/베이직 인증 끄기
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // JWT 필터 연결
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

