package com.thock.back.api.boundedContext.member.app;

import com.thock.back.api.boundedContext.member.domain.command.LoginCommand;
import com.thock.back.api.boundedContext.member.domain.entity.Credential;
import com.thock.back.api.boundedContext.member.domain.entity.LoginHistory;
import com.thock.back.api.boundedContext.member.domain.entity.Member;
import com.thock.back.api.boundedContext.member.domain.entity.RefreshToken;
import com.thock.back.api.boundedContext.member.in.dto.AuthenticationResult;
import com.thock.back.api.boundedContext.member.out.CredentialRepository;
import com.thock.back.api.boundedContext.member.out.LoginHistoryRepository;
import com.thock.back.api.boundedContext.member.out.MemberRepository;
import com.thock.back.api.boundedContext.member.out.RefreshTokenRepository;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import com.thock.back.api.global.security.JwtTokenProvider;
import com.thock.back.api.global.security.RefreshTokenHasher;
import com.thock.back.api.shared.member.domain.MemberState;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final MemberRepository memberRepository;
    private final CredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenHasher refreshTokenHasher;

    @Transactional
    public AuthenticationResult login(LoginCommand command) throws Exception {
        // Member 조회
        Member member = memberRepository.findByEmail(command.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // 탈퇴/비활성 계정 제한이 있다면 여기서 컷
        if (member.isWithdrawn()) {
            throw new CustomException(ErrorCode.MEMBER_WITHDRAWN);
        }

        // Credential 조회 + 비밀번호 검증
        Credential credential = credentialRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CREDENTIAL_NOT_FOUND));

        boolean passwordMatches = passwordEncoder.matches(command.password(), credential.getPasswordHash());
        if (!passwordMatches) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 로그인 성공 처리
        member.recordLogin();
        memberRepository.save(member);

        // JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getRole(),
                member.getState()
        );

        // 기존 RefreshToken 폐기
        refreshTokenRepository.revokeAllByMemberId(member.getId(), LocalDateTime.now());

        // 새 RefreshToken 발급 & 저장 (해시 적용)
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(member.getId());

        // 평문을 SHA-256 해시로 변환
        String tokenHash = refreshTokenHasher.hash(refreshTokenValue);

        RefreshToken refreshToken = RefreshToken.issue(
                member.getId(),
                tokenHash,
                LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpSeconds())
        );

        refreshTokenRepository.save(refreshToken);

        // 로그인 이력 저장
        loginHistoryRepository.save(LoginHistory.success(member.getId()));

        return AuthenticationResult.of(accessToken, refreshTokenValue);
    }

    @Transactional
    public AuthenticationResult refreshAccessToken(String refreshTokenValue) throws Exception {

        // 평문을 SHA-256 해시로 변환
        String tokenHash = refreshTokenHasher.hash(refreshTokenValue);

        // RefreshToken 조회
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("[SECURITY] Refresh token not found in BD: hash={}",
                            tokenHash.substring(0, 10) + "...");
                    return new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
                });

        // RefreshToken 검증
        if (token.isRevoked()) {
            log.warn("[SECURITY] Revoke refresh token access attempt: member={}, revokedAt={}",
                    token.getMemberId(), token.getRevokedAt());
            throw new CustomException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }

        if (token.isExpired()) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        if (!jwtTokenProvider.validate(refreshTokenValue)) {
            log.warn("[SECURITY] Invalid RefreshToken JWT signature");
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long jwtMemberId = jwtTokenProvider.extractMemberId(refreshTokenValue);

        if (!token.getMemberId().equals(jwtMemberId)) {
            log.warn("[SECURITY] RefreshToken memberId mismatch: DB={}, JWT={}",
                    token.getMemberId(), jwtMemberId);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Member member = memberRepository.findById(token.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 계정 상태 검증
        if (member.isWithdrawn()) {
            log.warn("[SECURITY] Withdrawn member tried to refresh token. memberId={}", member.getId());
            throw new CustomException(ErrorCode.MEMBER_WITHDRAWN);
        }

        if (member.isInActive()) {
            throw new CustomException(ErrorCode.MEMBER_INACTIVE);
        }

        // 기존 RefreshToken 폐기 (Refresh Token Rotation)
        token.revoke();
        refreshTokenRepository.save(token);

        // 새로운 토큰 세트 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(token.getMemberId(), member.getRole(), member.getState());
        String newRefreshTokenValue = jwtTokenProvider.createRefreshToken(token.getMemberId());
        String newTokenHash = refreshTokenHasher.hash(newRefreshTokenValue);

        RefreshToken newRefreshToken = RefreshToken.issue(
                token.getMemberId(),
                newTokenHash,
                LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpSeconds())
        );
        refreshTokenRepository.save(newRefreshToken);

        return AuthenticationResult.of(newAccessToken, newRefreshTokenValue);
    }
}
