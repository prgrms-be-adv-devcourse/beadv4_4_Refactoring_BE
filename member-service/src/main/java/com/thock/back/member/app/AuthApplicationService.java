package com.thock.back.member.app;

import com.thock.back.member.domain.service.MemberAuthenticator;
import com.thock.back.member.domain.service.RefreshTokenValidator;
import com.thock.back.member.domain.vo.TokenPair;
import com.thock.back.member.domain.vo.ValidatedRefreshToken;
import com.thock.back.member.infrastructure.history.LoginHistoryRecorder;
import com.thock.back.member.infrastructure.token.TokenIssuer;
import com.thock.back.member.domain.command.LoginCommand;
import com.thock.back.member.domain.entity.Member;
import com.thock.back.member.in.dto.AuthenticationResult;
import com.thock.back.member.out.MemberRepository;
import com.thock.back.member.out.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 Application Service (Orchestration Layer)
 * 책임:
 * - 도메인 서비스와 인프라 서비스를 조정
 * - 트랜잭션 경계 관리
 * - 워크플로우 정의
 * 변경 사항:
 * - 복잡한 로직을 Domain/Infrastructure Service로 위임
 * - 얇은 조정 계층(Thin Orchestration Layer)으로 변경
 **/

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    // Domain Services
    private final MemberAuthenticator memberAuthenticator;
    private final RefreshTokenValidator refreshTokenValidator;

    // Infrastructure Services
    private final TokenIssuer tokenIssuer;
    private final LoginHistoryRecorder loginHistoryRecorder;

    // Repositories
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 로그인 처리
     * 워크플로우:
     * 1. 회원 인증 (Domain Service)
     * 2. 로그인 성공 처리 (Domain Entity)
     * 3. 토큰 발급 (Infrastructure Service)
     * 4. 로그인 이력 기록 (Infrastructure Service, 비동기)
     * @param command = 로그인 커맨드
     * @return 인증 결과 (AccessToken + RefreshToken)
     **/

    @Transactional
    public AuthenticationResult login(LoginCommand command) {
        // 1. 회원 인증
        Member member = memberAuthenticator.authenticate(command);

        // 2. 로그인 성공 처리
        member.recordLogin();
        memberRepository.save(member);

        // 3. 토큰 발급
        TokenPair tokens = tokenIssuer.issueTokens(member);

        // 로그인 이력 저장 (비동기)
        loginHistoryRecorder.recordSuccess(member.getId());

        log.info("[AUTH] Login successful. memberId={}, email={}",
                member.getId(), member.getEmail());

        return AuthenticationResult.of(tokens.accessToken(), tokens.refreshToken());
    }

    /**
     * RefreshToken을 이용한 토큰 재발급 (Refresh Token Rotation)
     * 워크플로우:
     * 1. RefreshToken 검증 (Domain Service)
     * 2. 기존 RefreshToken 폐기
     * 3. 새 토큰 쌍 발급 (Infrastructure Service)
     * @param refreshTokenValue = 평문 RefreshToken
     * @return 인증 결과 (새로운 AccessToken + RefreshToken)
     **/
    @Transactional
    public AuthenticationResult refreshAccessToken(String refreshTokenValue) {
        // 1. RefreshToken 검증
        ValidatedRefreshToken validated = refreshTokenValidator.validate(refreshTokenValue);

        // 2. 기존 RefreshToken 폐기 (Refresh Token Rotation)
        validated.token().revoke();
        refreshTokenRepository.save(validated.token());

        // 3. 새 토큰 쌍 발급
        TokenPair tokens = tokenIssuer.issueTokens(validated.member());

        log.info("[AUTH] AccessToken refreshed. memberId={}",
                validated.member().getId());

        return AuthenticationResult.of(tokens.accessToken(), tokens.refreshToken());
    }
}
