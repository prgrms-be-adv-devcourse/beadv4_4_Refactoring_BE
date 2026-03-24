package com.thock.back.global.security;

import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;

public interface JwtValidator {
    boolean validate(String token);
    Long extractMemberId(String token);
    MemberRole extractRole(String token);
    MemberState extractState(String token);
}
