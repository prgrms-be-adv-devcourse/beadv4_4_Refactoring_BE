package com.thock.back.global.outbox.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxStatus {
    PENDING("발행 대기"),
    PROCESSING("발행 중"),    // 이벤트 claim 상태 (중복 발행 방지)
    SENT("발행 완료"),
    FAILED("발행 실패");

    private final String description;
}
