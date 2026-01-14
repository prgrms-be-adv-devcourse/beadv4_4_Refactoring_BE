package com.thock.back.api.global.exception;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException{
    private final String resultCode;
    private final String resultMessage;

    public GlobalException(String resultCode, String resultMessage) {
        super(resultCode + " " + resultMessage);
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
    }
}
