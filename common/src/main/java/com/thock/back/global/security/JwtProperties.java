package com.thock.back.global.security;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpSeconds,
        long refreshTokenExpSeconds,
        String issuer
) {}
