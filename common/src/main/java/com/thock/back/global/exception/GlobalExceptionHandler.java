package com.thock.back.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * 모든 예외를 잡아서 일관된 형태의 ErrorResponse로 변환
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     * 비즈니스 로직에서 발생한 예외 처리 -> 개발자가 비즈니스 로직에서 throw new CustomException 작성
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(
            CustomException e,
            HttpServletRequest request) {

        log.warn("CustomException 발생: code={}, message={}, path={}",
                e.getErrorCode().getCode(), e.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(e.getErrorCode(), request.getRequestURI());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }


    /**
     * @Valid 검증 실패 처리
     * @RequestBody에 @Valid를 사용했을 때 발생
     * CustomException 처럼 따로 클래스 만들어 줄 필요 없음
     * 컨트롤러에 @Valid 작성, 유효성 검증 실패한 경우 자동으로 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {

        log.warn("Validation 실패: path={}", request.getRequestURI());

        Map<String, String> details = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST,
                request.getRequestURI(),
                details
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * @Validated 검증 실패 처리 (파라미터 검증)
     * @RequestParam, @PathVariable에 검증 어노테이션 사용 시 발생
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request) {

        log.warn("파라미터 검증 실패: path={}", request.getRequestURI());

        Map<String, String> details = new HashMap<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            details.put(fieldName, errorMessage);
        }

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST,
                request.getRequestURI(),
                details
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 잘못된 HTTP 메서드 요청 처리
     * GET 엔드포인트에 POST 요청 등
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {

        log.warn("지원하지 않는 HTTP 메서드: method={}, path={}",
                e.getMethod(), request.getRequestURI());

        String message = String.format("지원하지 않는 HTTP 메서드입니다. 요청: %s", e.getMethod());
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST,
                request.getRequestURI(),
                message
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(errorResponse);
    }

    /**
     * 요청 본문 파싱 실패 처리
     * JSON 형식이 잘못되었을 때
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {

        log.warn("요청 본문 파싱 실패: path={}", request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST,
                request.getRequestURI(),
                "요청 데이터 형식이 올바르지 않습니다."
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 예상하지 못한 모든 예외 처리
     * NullPointerException, IllegalArgumentException 등 모든 예외의 최종 안전망
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e,
            HttpServletRequest request) {

        log.error("예상치 못한 예외 발생: path={}", request.getRequestURI(), e);

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

}