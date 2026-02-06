package com.thock.back.member.domain.vo;

import com.thock.back.member.domain.entity.Member;
import com.thock.back.member.domain.entity.RefreshToken;

public record ValidatedRefreshToken(
        RefreshToken token,
        Member member
) {
    public ValidatedRefreshToken {
        if (token == null) {
            throw new IllegalArgumentException("Refresh token must not be null");
        }
        if (member == null) {
            throw new IllegalArgumentException("Member must not be null");
        }
    }
}
