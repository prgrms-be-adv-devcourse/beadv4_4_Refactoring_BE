package com.thock.back.global.security;

import com.thock.back.global.security.context.AuthMember;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {

    private AuthContext() {}

    public static AuthMember get() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new Exception();
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AuthMember authMember)) {
            throw new Exception();
        }

        return authMember;
    }

    public static Long memberId() throws Exception {
        return get().memberId();
    }

    public static MemberRole role() throws Exception {
        return get().role();
    }

    public static MemberState state() throws Exception {
        return get().state();
    }
}
