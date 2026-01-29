package com.thock.back.api.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * 가장 많이 사용 될 코드
     * 우리가 ErrorCode에서 작성한 message를 그대로 사용하여 일관성 맞춤
      */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode와 원인 예외로 예외 생성 - 디버깅 시 유용
     * @param errorCode 에러 코드
     * @param cause 원인 예외
     */
    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    // ========== 특수한 경우에만 사용 (일반적으로 권장하지 않음) ==========

    /**
     * 커스텀 메시지와 함께 생성
     *
     * ⚠️ 팀원들 간 소통 없이 커스텀 메시지 작성 시 일관성 해칠 수 있음.
     * ErrorCode의 기본 메시지를 사용하는 것을 강력히 권장
     *
     *
     * 허용되는 사용 예시:
     * - 동적 정보를 포함해야 하는 특수한 경우
     *   throw new CustomException(ErrorCode.INVALID_REQUEST,
     *       "파일 크기가 제한을 초과했습니다: " + fileSize + "MB");
     *
     * @param errorCode 에러 코드
     * @param customMessage 커스텀 메시지 (ErrorCode의 기본 메시지를 오버라이드)
     */
    public CustomException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

}