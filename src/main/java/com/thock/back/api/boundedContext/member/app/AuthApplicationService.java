package com.thock.back.api.boundedContext.member.app;

import com.thock.back.api.boundedContext.member.domain.*;
import com.thock.back.api.boundedContext.member.out.CredentialRepository;
import com.thock.back.api.boundedContext.member.out.LoginHistoryRepository;
import com.thock.back.api.boundedContext.member.out.MemberRepository;
import com.thock.back.api.boundedContext.member.out.RefreshTokenRepository;
import com.thock.back.api.global.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final MemberRepository memberRepository;
    private final CredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;


    @Transactional
    public LoginResult login(LoginCommand command) throws Exception {
        // Member 조회
        Member member = memberRepository.findByEmail(command.email())
                .orElseThrow(() -> new Exception());

        // 탈퇴/비활성 계정 제한이 있다면 여기서 컷
        if (member.isWithdrawn()) throw new Exception();

        // Credential 조회 + 비밀번호 검증
        Credential credential = credentialRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new Exception());

        boolean ok = passwordEncoder.matches(command.password(), credential.getPasswordHash());
        if (!ok) throw new Exception(); // TODO: 로그인 실패 이력 남기고 싶다면 여기서 LoginHistory 저장 가능

        // 로그인 성공 처리
        member.recordLogin();
        memberRepository.save(member);

        // JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole(), member.getState());

        // 기존 RefreshToken 폐기
        List<RefreshToken> oldTokens = refreshTokenRepository.findAllByMemberIdAndRevokedAtIsNull(member.getId());
        oldTokens.forEach(RefreshToken::revoke);
        refreshTokenRepository.saveAll(oldTokens);

        // 새 RefreshToken 발급 & 저장
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(member.getId());
        RefreshToken refreshToken = RefreshToken.issue(
                member.getId(),
                refreshTokenValue,
                LocalDateTime.now()
        );
        refreshTokenRepository.save(refreshToken);

        // 로그인 이력 저장
        loginHistoryRepository.save(LoginHistory.success(member.getId()));

        return new LoginResult(accessToken, refreshTokenValue);
    }

    public String refreshAccessToken(String refreshTokenValue) throws Exception {
        RefreshToken token = refreshTokenRepository.findByTokenValue(refreshTokenValue)
                .orElseThrow(() -> new Exception()); // TODO: 401

        if (token.getRevokedAt() != null) {
            throw new Exception(); // TODO: 401
        }

        Long memberId = jwtTokenProvider.extractMemberId(refreshTokenValue);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new Exception());

        return jwtTokenProvider.createAccessToken(memberId, member.getRole(), member.getState());
    }
}
