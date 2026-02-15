package com.thock.back.settlement.reconciliation.in.dto;

import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.shared.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemMessageDtoTest {

    @Test
    @DisplayName("[성공] 결제 완료(PAYMENT) 이벤트는 수량과 금액이 양수로 엔티티에 매핑된다")
    void toEntity_PaymentCompleted() {
        // given: 결제 완료 상태로 3개, 15000원짜리 DTO 생성
        OrderItemMessageDto dto = createDto("PAYMENT_COMPLETED", 3, 15000L);

        // when: 엔티티로 변환
        SalesLog entity = dto.toEntity();

        // then: 양수 그대로 들어가는지 검증
        assertThat(entity.getTransactionType()).isEqualTo(TransactionType.PAYMENT);
        assertThat(entity.getProductQuantity()).isEqualTo(3);
        assertThat(entity.getPaymentAmount().amount()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("[성공] 환불(REFUND) 이벤트는 수량과 금액이 무조건 음수로 변환되어 매핑된다")
    void toEntity_RefundCompleted() {
        // given: 주문 모듈에서 실수로 양수로 보냈다고 가정 (수량 1, 금액 5000)
        OrderItemMessageDto dto = createDto("REFUND_COMPLETED", 1, 5000L);

        // when: 엔티티로 변환
        SalesLog entity = dto.toEntity();

        // then: 강제로 음수(-) 처리되었는지 검증!! (핵심)
        assertThat(entity.getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(entity.getProductQuantity()).isEqualTo(-1);
        assertThat(entity.getPaymentAmount().amount()).isEqualTo(-5000L);
    }

    // 테스트용 DTO 생성 편의 메서드
    private OrderItemMessageDto createDto(String eventType, int qty, Long amount) {
        return new OrderItemMessageDto(
                "ORD-001", 1L, 100L, "키보드",
                qty, 5000L, amount, eventType, null, LocalDateTime.now()
        );
    }
}
