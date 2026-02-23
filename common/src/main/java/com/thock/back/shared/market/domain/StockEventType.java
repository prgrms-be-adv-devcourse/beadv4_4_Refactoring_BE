package com.thock.back.shared.market.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockEventType {
    RESERVE("RESERVE"),             // 예약 재고 증가
    RELEASE("RELEASE"),             // 예약 재고 해제
    COMMIT("COMMIT");               // 실제 재고 차감 및 예약 재고 해제

    private final String value;
}
