package com.thock.back.global.security;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.security.context.AuthMember;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {

    private AuthContext() {}

    public static AuthMember get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AuthMember authMember)) {
            throw new CustomException(ErrorCode.INVALID_PRINCIPAL_TYPE);
        }

        return authMember;
    }

    public static Long memberId() {
        return get().memberId();
    }

    public static MemberRole role() {
        return get().role();
    }

    public static MemberState state() {
        return get().state();
    }
}
