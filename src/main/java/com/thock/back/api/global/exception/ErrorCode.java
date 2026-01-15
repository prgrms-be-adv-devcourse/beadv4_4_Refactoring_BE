package com.thock.back.api.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ===== 공통 에러 =====
    INTERNAL_SERVER_ERROR("500", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ;


    // ===== JWT 인증 관련 에러 =====


    // ===== 사용자 관련 에러 =====


    private final String resultCode;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String resultCode, String message, HttpStatus httpStatus) {
        this.resultCode = resultCode;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
