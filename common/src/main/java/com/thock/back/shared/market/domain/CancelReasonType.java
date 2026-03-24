package com.thock.back.shared.market.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 취소 사유
@Getter
@RequiredArgsConstructor
public enum CancelReasonType {

    // 시스템에서 취소하는 경우
    PAYMENT_TIMEOUT("결제 시간 초과"),
    PAYMENT_CANCELLED_BY_USER("사용자 결제 취소"),

    // 사용자가 취소하는 경우
    CHANGE_OF_MIND("단순 변심"),
    DELIVERY_DELAY("배송 지연"),
    PRODUCT_DEFECT("상품 불량"),
    WRONG_OPTION("옵션 잘못 선택"),
    ETC("기타"),

    /**
     * 복합 사유 (Order 요약용)
     * 사실 지금 상태에서 복합 사유 일어날 일은 없는데 확장용으로 작성
     * 한 번 부분 환불이 진행되면 나머지 품목들은 바로 구매 확정 처리가 되어야한다.
     */
    MIXED("복합 사유");

    private final String description;

    /**
     * 시스템에 의한 취소인지 확인
     */
    public boolean isSystemCancellation() {
        return this == PAYMENT_TIMEOUT || this == PAYMENT_CANCELLED_BY_USER;
    }

}
