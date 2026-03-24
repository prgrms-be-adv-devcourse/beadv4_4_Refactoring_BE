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
     * 구매 확정 전까지 취소 가능 (배송 후에도 7일 이내 취소 가능)
     */
    public boolean isCancellable() {
        return this == PENDING_PAYMENT ||
                this == PAYMENT_COMPLETED ||
                this == PREPARING ||
                this == SHIPPING ||
                this == DELIVERED;
    }

    /**
     * 구매 확정 가능한 상태인지 확인
     */
    public boolean isConfirmable() {
        return this == DELIVERED;
    }

    /**
     * 주문을 취소하고 나서의 상태
     * 환불 완료로 변경 가능한 상태 (배송 전 / 배송 후)
     */
    public boolean canCompleteRefund(){
        return this == CANCELLED || this == REFUND_REQUESTED;
    }

    /**
     * 배송 후 환불 요청 가능한 상태인지 확인
     */
    public boolean isRefundable() {
        return this == DELIVERED;
    }

    /**
     * 정상 진행 중인 상태 (취소/환불/구매확정 아님)
     * 부분 환불 시 나머지 아이템 강제 구매확정 대상 판별용
     */
    public boolean isActiveState() {
        return  this == PAYMENT_COMPLETED || // 결제 완료
                this == PREPARING || // 배송 준비중
                this == SHIPPING || // 배송중
                this == DELIVERED; // 배송 완료
    }

}
