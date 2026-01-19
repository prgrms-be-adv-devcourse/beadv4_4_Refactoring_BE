package com.thock.back.api.global.security;

import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;

public record JwtPrincipal(
        Long memberId,
        MemberRole role,
        MemberState state
) {}
