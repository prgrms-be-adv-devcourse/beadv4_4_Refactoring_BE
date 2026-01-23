package com.thock.back.api.boundedContext.member.in.dto;

import com.thock.back.api.shared.member.domain.MemberRole;

public record UpdateNameRequest(
        MemberRole role
) {}
