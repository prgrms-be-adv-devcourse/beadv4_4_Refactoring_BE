package com.thock.back.market.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Settlement 정산 이벤트 타입
 * Settlement의 OrderEventStatus와 호환되는 값으로 정의
 */
@Getter
@RequiredArgsConstructor
public enum SettlementEventType {
    PAYMENT_COMPLETED("PAYMENT_COMPLETED"),
    PURCHASE_CONFIRMED("PURCHASE_CONFIRMED"),
    REFUND_COMPLETED("REFUND_COMPLETED");

    private final String value;

    @Override
    public String toString() {
        return value;
    }
}
