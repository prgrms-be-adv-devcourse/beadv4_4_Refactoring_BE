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
    SELLER_REQUIRED("PRODUCT-400-1", "판매자 ID는 필수입니다", HttpStatus.BAD_REQUEST),
    PRODUCT_NAME_REQUIRED("PRODUCT-400-2", "상품명은 필수입니다.", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_INVALID("PRODUCT-400-3", "상품가격은 0원 이상이어야 합니다", HttpStatus.BAD_REQUEST),
    PRODUCT_CATEGORY_REQUIRED("PRODUCT-400-4", "카테고리 설정은 필수입니다.", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND("PRODUCT-400-1", "존재하지 않는 상품입니다.", HttpStatus.NOT_FOUND),

    // ===== 장바구니 =====
    CART_ITEM_NOT_FOUND("CART-404-1", "장바구니에 해당 상품이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    // ===== 주문 =====

    // ===== 배송 =====

    // ===== 결제 =====
    WALLET_NOT_FOUND("WALLET-404-1", "지갑을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    WALLET_IS_LOCKED("WALLET-404-2", "이 지갑은 현재 정지 된 상태입니다.", HttpStatus.NOT_FOUND),
    WALLET_NOT_WITHDRAW("WALLET-404-3", "잔액이 부족합니다.", HttpStatus.NOT_FOUND),
    TOSS_EMPTY_RESPONSE("PAYMENT-400-1", "토스 결제 승인 응답 바디가 비었습니다.", HttpStatus.BAD_REQUEST),
    TOSS_HTTP_ERROR("PAYMENT-400-2", "토스 결제 승인 HTTP 호출 실패", HttpStatus.BAD_REQUEST),
    TOSS_UNKNOWN_CODE("PAYMENT-400-3", "토스 결제 승인 중 알 수 없는 오류 발생", HttpStatus.BAD_REQUEST),
    TOSS_CONFIRM_FAIL("PAYMENT-400-4", "토스 결제 승인 실패", HttpStatus.BAD_REQUEST),
    TOSS_CALL_EXCEPTION("PAYMENT-400-5", "토스 결제 승인 호출 중 예외", HttpStatus.BAD_REQUEST),
    TOSS_AMOUNT_NOT_MATCH("PAYMENT-400-6", "결제 금액이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    TOSS_ORDER_NOT_MATCH("PAYMENT-400-7", "주문번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    TOSS_MISSING_FIELDS("PAYMENT-400-8", "토스 PG 응답 필드 부족", HttpStatus.BAD_REQUEST),
    TOSS_REJECTED("PAYMENT-402-1", "PG에서 결제가 거절되었습니다.", HttpStatus.PAYMENT_REQUIRED);
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