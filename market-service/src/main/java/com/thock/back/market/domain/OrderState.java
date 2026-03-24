package com.thock.back.market.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderState {
    PENDING_PAYMENT("결제 대기"),
    PAYMENT_COMPLETED("결제 완료"),
    PARTIALLY_SHIPPED("부분 배송"),
    PREPARING("배송 준비중"),
    SHIPPING("배송중"),
    DELIVERED("배송 완료"),
    CONFIRMED("구매 확정"),
    PARTIALLY_CANCELLED("부분 취소"),
    CANCELLED("취소됨"),
    PARTIALLY_REFUNDED("부분 환불 완료"),
    REFUNDED("환불 완료");

    private final String description;

    /**
     * 취소/환불 가능한 상태인지 확인
     * PENDING_PAYMENT : 취소 처리만 하면 됨
     * PAYMENT_COMPLETED, PREPARING : 환불 처리 필요
     * SHIPPING, DELIVERED : 배송 완료 후 7일 이내 + 구매확정 전까지 환불 가능
     * CONFIRMED 이후 : CS/전화 처리
     */
    public boolean isCancellable(){
        return this == PENDING_PAYMENT ||
                this == PAYMENT_COMPLETED ||
                this == PREPARING ||
                this == SHIPPING ||
                this == DELIVERED;
    }

    // 구매 확정 가능한 상태인지 확인
    public boolean isConfirmable() {
        return this == DELIVERED;
    }

    // 환불 완료 가능한 상태인지 체크 (취소된 주문만 환불 완료 처리 가능)
    public boolean canCompleteRefund() {
        return this == CANCELLED || this == PARTIALLY_CANCELLED;
    }
}
