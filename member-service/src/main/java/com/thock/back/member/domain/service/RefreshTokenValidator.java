package com.thock.back.member.domain.service;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.member.domain.entity.Member;
import com.thock.back.member.domain.entity.RefreshToken;
import com.thock.back.member.domain.vo.ValidatedRefreshToken;
import com.thock.back.member.out.MemberRepository;
import com.thock.back.member.out.RefreshTokenRepository;
import com.thock.back.member.security.JwtTokenProvider;
import com.thock.back.member.security.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RefreshToken 검증을 담당하는 Domain Service
 * 책임:
 * - RefreshToken 존재 여부 확인
 * - 토큰 상태 검증 (폐기, 만료)
 * - JWT 서명 검증
 * - MemberId 일치 검증
 * - 회원 상태 검증
 * 위치 이유:
 * - 복잡한 검증 로직을 캡슐화
 * - RefreshToken, Member 엔티티와 JWT 검증을 조합
 **/

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenValidator {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    /**
     * RefreshToken을 종합적으로 검증
     * @param refreshTokenValue = 평문 RefreshToken
     * @return 검증된 토큰과 회원 정보
     * @throws CustomException 검증 실패 시
     **/
    public ValidatedRefreshToken validate(String refreshTokenValue) {
        String tokenHash = refreshTokenHasher.hash(refreshTokenValue);

        RefreshToken token = findToken(tokenHash);
        validateTokenState(token);
        validateJwtSignature(refreshTokenValue);
        validateMemberIdMatch(token, refreshTokenValue);
        Member member = findAndValidateMember(token.getMemberId());

        log.info("[AUTH] RefreshToken validated successfully. memberId={}", member.getId());

        return new ValidatedRefreshToken(token, member);
    }

    private RefreshToken findToken(String tokenHash) {
        return refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("[SECURITY] RefreshToken not found in DB. hash={}...",
                            tokenHash.substring(0, 10));
                    return new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
                });
    }

    private void validateTokenState(RefreshToken token) {
        if (token.isRevoked()) {
            log.warn("[SECURITY] Revoked RefreshToken access attempt. memberId={}, revokedAt={}",
                    token.getMemberId(), token.getRevokedAt());
            throw new CustomException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }

        if (token.isExpired()) {
            log.warn("[SECURITY] Expired RefreshToken access attempt. memberId={}, expiredAt={}",
                    token.getMemberId(), token.getExpiresAt());
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
    }

    private void validateJwtSignature(String refreshTokenValue) {
        if (!jwtTokenProvider.validate(refreshTokenValue)) {
            log.warn("[SECURITY] Invalid RefreshToken JWT signature");
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    private void validateMemberIdMatch(RefreshToken token, String refreshTokenValue) {
        Long jwtMemberId = jwtTokenProvider.extractMemberId(refreshTokenValue);

        if (!token.getMemberId().equals(jwtMemberId)) {
            log.warn("[SECURITY] RefreshToken memberId mismatch. DB={}, JWT={}",
                    token.getMemberId(), jwtMemberId);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    private Member findAndValidateMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.isWithdrawn()) {
            log.warn("[Security] Withdrawn member tried to refresh token. memberId={}", member.getId());
            throw new CustomException(ErrorCode.MEMBER_WITHDRAWN);
        }

        if (member.isInActive()) {
            log.warn("[Security] Inactive member tried to refresh token. memberId={}", member.getId());
            throw new CustomException(ErrorCode.MEMBER_INACTIVE);
        }

        return member;
    }
}
