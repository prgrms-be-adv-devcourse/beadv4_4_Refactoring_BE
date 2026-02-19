package com.thock.back.global.outbox.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxStatus {
    PENDING("발행 대기"),       // 대기: 생성됨, 발송 전
    PROCESSING("발행 중"),     // 처리중: 발송 시도 중, 이벤트 claim 상태 (중복 발행 방지)
    SENT("발행 완료"),          // 성공: 발송 완료
    FAILED("발행 실패");        // 실패: 발송 실패

    private final String description;
}
