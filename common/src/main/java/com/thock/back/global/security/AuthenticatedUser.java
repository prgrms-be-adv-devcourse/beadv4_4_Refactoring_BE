package com.thock.back.global.security;

import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;

/**
 * Gateway에서 검증된 인증 정보를 담는 객체
 * HTTP 헤더 (X-Member-Id, X-Member-Role, X-Member-State)에서 추출
 */
public record AuthenticatedUser(
    Long memberId,
    MemberRole role,
    MemberState state
) {
    public static AuthenticatedUser of(Long memberId, MemberRole role, MemberState state) {
        return new AuthenticatedUser(memberId, role, state);
    }
}