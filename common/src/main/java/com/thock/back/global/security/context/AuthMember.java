package com.thock.back.global.security.context;

import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;

public record AuthMember(
        Long memberId,
        MemberRole role,
        MemberState state
) {}
