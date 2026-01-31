package com.thock.back.payment.domain;

public enum PaymentStatus {
    REQUESTED,   // 주문에서 결제 요청 받은 상태
    PG_PENDING,  // PG 결제 진행 중
    PG_PAID,     // PG 결제 완료
    COMPLETED,   // 지갑까지 모두 반영 완료
    FAILED,      // 실패
    CANCELED     // 취소
}
