package com.thock.back.api.boundedContext.market.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderState {
    PENDING_PAYMENT("결제 대기"),      // 주문 생성 직후
    PAYMENT_COMPLETED("결제 완료"),    // 결제 완료
    PREPARING("배송 준비중"),          // 상품 준비중
    SHIPPING("배송중"),                // 배송 시작
    DELIVERED("배송 완료"),            // 배송 완료
    CONFIRMED("구매 확정"),            // 구매자가 확정
    CANCELLED("취소됨"),               // 주문 취소
    REFUNDED("환불 완료");             // 환불 완료

    private final String description;

    /**
     * 취소 가능한 상태인지 확인
     * PENDING_PAYMENT : 취소 처리만 하면 됨
     * PAYMENT_COMPLETED, PREPARING : 환불 처리 필요
     * SHIPPING, DELIVERED : 반품 및 환불 필요 -> 며칠 이내 반품만 가능(Policy 필요)
     */
    public boolean isCancellable(){
        return this == PENDING_PAYMENT || this == PAYMENT_COMPLETED || this == PREPARING;
    }

    // 구매 확정 가능한 상태인지 확인
    public boolean isConfirmable() {
        return this == DELIVERED;
    }

}
