package com.thock.back.api.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 프로젝트 전체에서 사용되는 에러 코드를 관리하는 Enum 클래스
 *
 * [코드 번호 규칙]
 * - 400-x: Bad Request (잘못된 요청)
 * - 401-x: Unauthorized (인증 필요)
 * - 403-x: Forbidden (권한 없음)
 * - 404-x: Not Found (리소스 없음)
 * - 409-x: Conflict (리소스 충돌)
 * - 500-x: Internal Server Error (서버 오류)
 */
@Getter
public enum ErrorCode {

    // ===== 공통 =====
    INTERNAL_SERVER_ERROR("GLOBAL-500-1", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("GLOBAL-400-1", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),

    // ===== 인증 =====

    // ===== 회원 =====
    USER_NOT_FOUND("USER-404-1", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USER-409-1", "이미 존재하는 사용자입니다.", HttpStatus.CONFLICT),

    // ===== 상품 =====

    // ===== 장바구니 =====

    // ===== 주문 =====

    // ===== 배송 =====

    // ===== 결제 =====

    // ===== 정산 =====

    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }


    public int getStatus() {
        return httpStatus.value(); // ex) 404
    }
}