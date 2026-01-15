package com.thock.back.api.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 표준화된 에러 응답 DTO
 * 모든 예외 처리에서 일관된 형태의 에러 응답을 제공
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 JSON에서 제외
public class ErrorResponse {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String code;           // 에러 코드 (예: "404-2")
    private final String message;        // 에러 메시지
    private final String path;           // 경로 - 디버깅, 로깅, 모니터링에 유용
    private final String timestamp;      // 발생 시간
    private final Map<String, String> details;  // Validation 오류 상세 (선택적)

    /**
     * ErrorCode로부터 ErrorResponse 생성 (기본)
     */
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }

    /**
     * ErrorCode와 details로부터 ErrorResponse 생성 (Validation 오류용)
     */
    public static ErrorResponse of(ErrorCode errorCode, String path, Map<String, String> details) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .details(details)
                .build();
    }

    /**
     * 커스텀 메시지와 함께 ErrorResponse 생성
     *
     */
    public static ErrorResponse of(ErrorCode errorCode, String path, String customMessage) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(customMessage)
                .path(path)
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }
}