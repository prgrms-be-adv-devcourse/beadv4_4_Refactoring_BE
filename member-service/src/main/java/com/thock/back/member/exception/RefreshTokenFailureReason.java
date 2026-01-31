package com.thock.back.member.exception;

import lombok.Getter;

@Getter
public enum RefreshTokenFailureReason {
    EXPIRED("만료됨"),
    MALFORMED("형식 오류"),
    SIGNATURE_INVALID("서명 오류"),
    REVOKED("폐기됨"),
    NOT_FOUND("존재하지 않음");

    private final String description;

    RefreshTokenFailureReason(String description) {
        this.description = description;
    }
}
