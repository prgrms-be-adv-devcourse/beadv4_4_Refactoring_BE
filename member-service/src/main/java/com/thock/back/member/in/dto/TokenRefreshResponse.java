package com.thock.back.member.in.dto;

/**
 * Token Refresh API 응답 DTO
 *
 * Refresh Token Rotation 패턴 적용으로 인해 새로운 AccessToken과 RefreshToken을 모두 반환
 */
public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {}
