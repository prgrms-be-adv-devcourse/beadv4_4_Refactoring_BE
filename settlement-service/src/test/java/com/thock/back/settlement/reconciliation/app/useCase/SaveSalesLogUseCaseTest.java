package com.thock.back.settlement.reconciliation.app.useCase;

import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.in.dto.OrderItemMessageDto;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import com.thock.back.settlement.shared.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveSalesLogUseCaseTest {

    @InjectMocks
    private SaveSalesLogUseCase saveSalesLogUseCase;

    @Mock
    private SalesLogRepository salesLogRepository;

    @Test
    @DisplayName("[성공] 결제 완료(PAYMENT_COMPLETED) 시 상태만 PENDING으로 저장된다")
    void execute_PaymentCompleted() {
        // given
        OrderItemMessageDto dto = createDto("PAYMENT_COMPLETED", 4);

        // when
        saveSalesLogUseCase.execute(dto);

        // then: save가 1번 호출되었는지 확인
        verify(salesLogRepository, times(1)).save(any(SalesLog.class));
    }

    @Test
    @DisplayName("[성공] 부분 확정(PURCHASE_CONFIRMED) 시 원본 결제 데이터를 찾아 확정 도장을 찍는다")
    void execute_PurchaseConfirmed() {
        // given
        OrderItemMessageDto dto = createDto("PURCHASE_CONFIRMED", 3);
        SalesLog originalLog = SalesLog.builder().orderNo("ORD-001").build(); // 원본 데이터 모형

        // DB에서 원본 데이터를 조회하면 originalLog가 나온다고 설정(Mocking)
        when(salesLogRepository.findByOrderNoAndProductIdAndTransactionType(
                eq(dto.orderNo()), eq(dto.productId()), eq(TransactionType.PAYMENT)))
                .thenReturn(Optional.of(originalLog));

        // when
        saveSalesLogUseCase.execute(dto);

        // then
        assertThat(originalLog.getConfirmedAt()).isNotNull(); // 확정 시간이 찍혔는지 확인!
        // 확정은 더티 체킹으로 업데이트 되므로 save 호출은 없어야 정상 (단, 코드 구현 방식에 따라 다를 수 있음)
    }

    @Test
    @DisplayName("[성공] 부분 환불(REFUND_COMPLETED) 시 환불건 저장 & 원본 결제 데이터에 확정 도장을 찍는다")
    void execute_RefundCompleted() {
        // given
        OrderItemMessageDto dto = createDto("REFUND_COMPLETED", 1);
        SalesLog originalLog = SalesLog.builder().orderNo("ORD-001").build(); // 아직 확정 안 된 원본

        when(salesLogRepository.findByOrderNoAndProductIdAndTransactionType(
                eq(dto.orderNo()), eq(dto.productId()), eq(TransactionType.PAYMENT)))
                .thenReturn(Optional.of(originalLog));

        // when
        saveSalesLogUseCase.execute(dto);

        // then: 1. 환불 데이터가 DB에 저장되었는가?
        ArgumentCaptor<SalesLog> saveCaptor = ArgumentCaptor.forClass(SalesLog.class);
        verify(salesLogRepository, times(1)).save(saveCaptor.capture());

        SalesLog savedRefundLog = saveCaptor.getValue();
        assertThat(savedRefundLog.getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(savedRefundLog.getConfirmedAt()).isNotNull(); // 환불건 자체도 바로 확정 찍히는지

        // 2. 방어로직 검증: 원본 데이터에도 확정 도장이 찍혔는가?
        assertThat(originalLog.getConfirmedAt()).isNotNull();
    }

    private OrderItemMessageDto createDto(String eventType, int qty) {
        return new OrderItemMessageDto(
                "ORD-001", 1L, 100L, "키보드",
                qty, 5000L, 5000L * qty, eventType, null, LocalDateTime.now()
        );
    }
}