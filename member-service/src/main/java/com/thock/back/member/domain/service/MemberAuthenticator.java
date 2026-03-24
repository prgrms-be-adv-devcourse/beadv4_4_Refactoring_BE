package com.thock.back.member.domain.service;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.member.domain.command.LoginCommand;
import com.thock.back.member.domain.entity.Credential;
import com.thock.back.member.domain.entity.Member;
import com.thock.back.member.out.CredentialRepository;
import com.thock.back.member.out.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 회원 인증을 담당하는 Domain Service
 * 책임:
 * - 이메일/비밀번호 기반 인증
 * - 회원 상태 검증 (탈퇴, 비활성)
 * - Credential 검증
 * 위치 이유:
 * - 여러 엔티티(Member, Credential)에 걸친 도메인 로직
 * - 순수한 도메인 규칙 (비즈니스 로직)
 **/

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberAuthenticator {

    private final MemberRepository memberRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 이메일과 비밀번호로 회원 인증
     *
     * @param command = 로그인 커맨드
     * @return 인증된 회원
     * @throws CustomException 인증 실패 시
     **/
    public Member authenticate(LoginCommand command) {
        Member member = findMemberByEmail(command.email());
        validateMemberState(member);
        validateCredential(member.getId(), command.password());

        log.info("[AUTH] Member authenticated successfully. memberId={}, email={}",
                member.getId(), member.getEmail());

        return member;
    }

    // Email로 회원 조회 및 존재 여부 검증
    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[AUTH] Login attempt with non-existent email: {}", email);
                    return new CustomException(ErrorCode.INVALID_CREDENTIALS);
                });
    }

    // 계정 상태 검증
    private void validateMemberState(Member member) {
        if (member.isWithdrawn()) {
            log.warn("[AUTH] Withdrawn member login attempt. memberId={}", member.getId());
            throw new CustomException(ErrorCode.MEMBER_WITHDRAWN);
        }

        if (member.isInActive()) {
            log.warn("[AUTH] Inactive member login attempt. memberId={}", member.getId());
            throw new CustomException(ErrorCode.MEMBER_INACTIVE);
        }
    }

    private void validateCredential(Long memberId, String password) {
        Credential credential = credentialRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CREDENTIAL_NOT_FOUND));

        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            log.warn("[AUTH] Invalid password attempt. memberId={}", memberId);
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
