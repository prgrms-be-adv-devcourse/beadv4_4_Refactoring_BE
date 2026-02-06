package com.thock.back.member.domain.vo;

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
