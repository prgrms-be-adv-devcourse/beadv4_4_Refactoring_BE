package com.thock.back.member.domain.vo;

/**
 * Access Token과 Refresh Token을 함께 관리하는 Value Object
 * - 불변 객체
 * - 토큰 발급의 결과를 표현
 **/

public record TokenPair (
        String accessToken,
        String refreshToken
) {
    public TokenPair {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token must not be null or blank");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be null or blank");
        }
    }
}
