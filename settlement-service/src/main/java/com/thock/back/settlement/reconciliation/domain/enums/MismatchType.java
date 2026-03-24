package com.thock.back.settlement.reconciliation.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MismatchType {

    PG_ONLY("PG사에는 존재하나 내부 DB에 없음 (누락)"),
    INTERNAL_ONLY("내부 DB에는 존재하나 PG사에 없음 (가짜/실패 주문)"),
    AMOUNT_DIFF("주문은 양쪽에 존재하나 결제 금액이 다름"),
    STATUS_DIFF("주문은 양쪽에 존재하나 결제 상태가 다름 (예: 결제완료 vs 취소)");

    private final String description;
}