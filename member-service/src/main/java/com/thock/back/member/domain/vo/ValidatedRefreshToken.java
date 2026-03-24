package com.thock.back.member.domain.vo;

import com.thock.back.member.domain.entity.Member;
import com.thock.back.member.domain.entity.RefreshToken;

/**
 * 검증이 완료된 RefreshToken과 해당 Member를 함께 관리하는 Value Object
 * - RefreshTokenValidator의 검증 결과
 * - 검증된 데이터만 포함
 **/

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
