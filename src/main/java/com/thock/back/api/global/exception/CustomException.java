package com.thock.back.api.global.exception;

public class CustomException extends RuntimeException{
    private final ErrorCode errorCode;
//    private final String message;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;

//        this.message = message;
    }


}
