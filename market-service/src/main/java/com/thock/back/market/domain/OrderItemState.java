package com.thock.back.market.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderItemState {
    // 결제 전/중
    PENDING_PAYMENT("결제 대기"),

    // 결제 완료 후
    PAYMENT_COMPLETED("결제 완료"),
    PREPARING("상품 준비중"),
    SHIPPING("배송중"),
    DELIVERED("배송 완료"),

    // 구매 확정 (정산 가능)
    CONFIRMED("구매 확정"),

    // 취소/환불
    CANCELLED("취소됨"), // 배송 전 : 주문 취소하면 반품 불필요 / 즉시 환불
    REFUND_REQUESTED("환불 요청"),  // 배송 후 : 반품을 필요, 반품 확인 후 환불
    REFUNDED("환불 완료");

    private final String description;

    /**
     * 주문 취소 가능한 상태인지 확인
     * 결제 대기
     * 결제 완료
     * 상품 준비중
     */
    public boolean isCancellable() {
        return this == PENDING_PAYMENT ||
                this == PAYMENT_COMPLETED ||
                this == PREPARING;
    }

    /**
     * 구매 확정 가능한 상태인지 확인
     */
    public boolean isConfirmable() {
        return this == DELIVERED;
    }

    /**
     * 환불 완료로 변경 가능한 상태 (배송 전 / 배송 후)
     */
    public boolean canCompleteRefund(){
        return this == CANCELLED || this == REFUND_REQUESTED;
    }

    /**
     * 배송 후 환불 요청 가능한 상태인지 확인
     */
    public boolean isRefundable() {
        return this == DELIVERED ||
                this == CONFIRMED;
    }
}
