package com.thock.back.member.in.dto;

import com.thock.back.shared.member.domain.MemberRole;

public record MemberInfoResponse (
        Long memberId,
        MemberRole role
) {}
