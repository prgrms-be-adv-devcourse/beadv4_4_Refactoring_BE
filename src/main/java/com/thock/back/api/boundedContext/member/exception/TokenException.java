package com.thock.back.api.boundedContext.member.exception;

import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;

public class TokenException extends CustomException {
    private final RefreshTokenFailureReason reason;  // 내부 로깅용

    public TokenException(ErrorCode errorCode, RefreshTokenFailureReason reason) {
        super(errorCode);
        this.reason = reason;
    }

    public RefreshTokenFailureReason getReason() {
        return reason;
    }
}
