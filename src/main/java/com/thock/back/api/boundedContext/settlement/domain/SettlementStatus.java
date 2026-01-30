package com.thock.back.api.boundedContext.settlement.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SettlementStatus {
    WAITING("지급 대기"),
    COMPLETE("지급 완료"),
    FAILED("지급 실패"),
    HOLD("지급 보류");

    private final String description;
}
