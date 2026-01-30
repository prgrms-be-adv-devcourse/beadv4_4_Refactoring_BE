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

    PAYMENT_TOSS_CONFIRM_FAILED("PAYMENT-400-1", "토스 결제 승인 실패", HttpStatus.BAD_REQUEST),
    PAYMENT_TOSS_EMPTY_RESPONSE("PAYMENT-500-1", "토스 결제 승인 응답이 비어있습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    PAYMENT_TOSS_CALL_EXCEPTION("PAYMENT-500-2", "토스 결제 승인 호출 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== 인증 =====
    INVALID_CREDENTIALS("AUTH-401-1", "이메일 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    MEMBER_WITHDRAWN("AUTH-401-2", "탈퇴한 회원입니다.", HttpStatus.UNAUTHORIZED),
    MEMBER_INACTIVE("AUTH-401-3", "비활성화된 계정입니다.", HttpStatus.UNAUTHORIZED),

    REFRESH_TOKEN_INVALID("AUTH-401-4", "유효하지 않은 Refresh Token 입니다.", HttpStatus.UNAUTHORIZED), // TODO: 보안을 위해 추후 해당 메세지로 고정
    REFRESH_TOKEN_REVOKED("AUTH-401-5", "폐기된 Refresh Token 입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("AUTH-401-6", "만료된 Refresh Token 입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("AUTH-404-1", "Refresh Token을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    AUTH_CONTEXT_NOT_FOUND("AUTH-401-7", "인증 정보를 찾을 수 없습니다. 로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    // ===== 회원 =====
    MEMBER_NOT_FOUND("MEMBER-404-1", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CREDENTIAL_NOT_FOUND("MEMBER-404-2", "인증 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    MEMBER_EMAIL_ALREADY_EXISTS("MEMBER-409-1", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),

    INVALID_ROLE_PROMOTION("MEMBER-400-1", "일반 회원만 판매자로 전환할 수 있습니다.", HttpStatus.BAD_REQUEST),
    SELLER_PROMOTION_REQUIRED_INFO("MEMBER-400-2", "판매자 전환에는 은행 계좌 정보가 필요합니다.", HttpStatus.BAD_REQUEST),

    INVALID_EMAIL_FORMAT("MEMBER-400-3", "올바른 이메일 형식이 아닙니다.", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_FORMAT("MEMBER-400-4", "비밀번호는 8자 이상, 영문과 숫자를 포함해야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_NAME_LENGTH("MEMBER-400-5", "이름은 2-50자 사이여야 합니다.", HttpStatus.BAD_REQUEST),

    // ===== 상품 =====
    SELLER_REQUIRED("PRODUCT-400-1", "판매자 ID는 필수입니다", HttpStatus.BAD_REQUEST),
    PRODUCT_NAME_REQUIRED("PRODUCT-400-2", "상품명은 필수입니다.", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_INVALID("PRODUCT-400-3", "상품가격은 0원 이상이어야 합니다", HttpStatus.BAD_REQUEST),
    PRODUCT_CATEGORY_REQUIRED("PRODUCT-400-4", "카테고리 설정은 필수입니다.", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND("PRODUCT-400-1", "존재하지 않는 상품입니다.", HttpStatus.NOT_FOUND),
    USER_FORBIDDEN("PRODUCT-403-1", "판매자만 상품을 등록할 수 있습니다.", HttpStatus.FORBIDDEN),
    SELLER_FORBIDDEN("PRODUCT-403-2", "본인의 상품만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN),

    // ===== 장바구니 =====
    CART_PRODUCT_OUT_OF_STOCK("CART-400-1", "선택하신 상품의 재고가 부족합니다.", HttpStatus.BAD_REQUEST),
    CART_EMPTY("CART-400-2", "장바구니가 비어있습니다.", HttpStatus.BAD_REQUEST),
    CART_USER_NOT_FOUND("CART-404-1", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CART_NOT_FOUND("CART-404-2", "장바구니를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND("CART-404-3", "장바구니에 해당 상품이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    CART_PRODUCT_INFO_NOT_FOUND("CART-404-4", "장바구니 상품의 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CART_PRODUCT_API_FAILED("CART-500-1", "상품 정보를 불러올 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== 주문 =====
    ORDER_BUYER_REQUIRED("ORDER-400-1", "구매자 정보가 필요합니다.", HttpStatus.BAD_REQUEST),
    ORDER_INVALID_STATE("ORDER-400-2", "주문 상태가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_CANCEL("ORDER-400-3", "주문 취소가 불가능한 상태입니다.", HttpStatus.BAD_REQUEST),
    ORDER_NO_ITEMS_SELECTED("ORDER-400-4", "주문할 상품을 선택해주세요.", HttpStatus.BAD_REQUEST),
    ORDER_USER_FORBIDDEN("ORDER-403-1", "주문 취소할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    ORDER_NOT_FOUND("ORDER-404-1", "주문을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ORDER_ITEM_NOT_FOUND("ORDER-404-1", "주문 상품을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
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
    PAYMENT_NOT_REQUEST("PAYMENT-400-9", "결제 상태가 요청이 아닙니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_COMPLETE("PAYMENT-400-10", "결제 상태가 완료가 아닙니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_UNKNOWN_ORDER_NUMBER("PAYMENT-404-1", "주문번호에 맞는 결제정보가 없습니다.", HttpStatus.NOT_FOUND),
    REFUND_NOT_CANCEL_REASON("REFUND-404-1", "환불 사유가 비어있습니다.", HttpStatus.NOT_FOUND),
    TOSS_REJECTED("PAYMENT-402-1", "PG에서 결제가 거절되었습니다.", HttpStatus.PAYMENT_REQUIRED),
    PAYMENT_NOT_MATCH_MEMBER("PAYMENT-400-11", "요청 멤버하고 결제 멤버하고 다릅니다.", HttpStatus.BAD_REQUEST);
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