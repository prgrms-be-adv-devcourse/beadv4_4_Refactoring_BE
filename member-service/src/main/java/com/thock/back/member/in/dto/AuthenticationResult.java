package com.thock.back.member.in.dto;

/**
 * 인증 성공 결과를 나타내는 도메인 DTO
 *
 * DDD 관점에서 Login과 Token Refresh는 모두 "인증 성공"이라는 동일한 도메인 이벤트의 결과
 * Application 계층에서 사용되며, Presentation 계층의 Response로 변환됨
 */
public record AuthenticationResult(
        String accessToken,
        String refreshToken
) {
    /**
     * 정적 팩토리 메서드
     */
    public static AuthenticationResult of(String accessToken, String refreshToken) {
        return new AuthenticationResult(accessToken, refreshToken);
    }
}