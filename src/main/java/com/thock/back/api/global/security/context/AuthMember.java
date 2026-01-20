package com.thock.back.api.global.security.context;

import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;

public record AuthMember(
        Long memberId,
        MemberRole role,
        MemberState state
) {}
