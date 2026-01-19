package com.thock.back.api.shared.auth;

public final class AuthContext {

    private static final ThreadLocal<Long> MEMBER_ID = new ThreadLocal<>();

    private AuthContext() {}

    public static void setMemberId(Long memberId) {
        MEMBER_ID.set(memberId);
    }

    public static Long getMemberId() {
        return MEMBER_ID.get();
    }

    public static void clear() {
        MEMBER_ID.remove();
    }
}
