package com.thock.back.api.productTest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

public class TokenMaker {

    @Test
    void 토큰_생성기() {
        // 아까 사진에 있던 그 비밀키
        String secret = "thock-2026-backend-authentication-jwt-secret-key-very-very-long-256bit";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject("1")               // memberId
                .claim("role", "USER")      // 권한
                .claim("state", "ACTIVE")   // 상태
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30)) // 30일 유효
                .signWith(key)
                .compact();

        System.out.println("====== 아래 값을 스웨거에 복사하세요 ======");
        System.out.println("Bearer " + token);
        System.out.println("========================================");
    }
}