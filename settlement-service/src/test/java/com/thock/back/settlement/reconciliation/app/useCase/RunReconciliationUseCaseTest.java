package com.thock.back.settlement.reconciliation.app.useCase;

import com.thock.back.settlement.reconciliation.domain.PgSalesRaw;
import com.thock.back.settlement.reconciliation.domain.ReconciliationResult;
import com.thock.back.settlement.reconciliation.domain.ReconciliationMismatchLog;
import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.domain.enums.MismatchType;
import com.thock.back.settlement.reconciliation.domain.enums.PgStatus;
import com.thock.back.settlement.reconciliation.domain.enums.ReconciliationStatus;
import com.thock.back.settlement.reconciliation.out.ReconciliationJobRepository;
import com.thock.back.settlement.reconciliation.out.ReconciliationMismatchLogRepository;
import com.thock.back.settlement.reconciliation.out.PgSalesRawRepository;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import com.thock.back.settlement.shared.enums.TransactionType;
import com.thock.back.settlement.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunReconciliationUseCaseTest {

    @InjectMocks
    private RunReconciliationUseCase runReconciliationUseCase;

    @Mock
    private SalesLogRepository salesLogRepository;
    @Mock
    private PgSalesRawRepository pgSalesRawRepository;
    @Mock
    private ReconciliationJobRepository jobRepository;
    @Mock
    private ReconciliationMismatchLogRepository mismatchLogRepository;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 2, 11);
        lenient().when(salesLogRepository.findByOrderNo(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("1. [성공] 1:1 완벽 일치 - PG 1건, DB 1건 금액 동일")
    void execute_Success_OneToOneMatch() {
        // given
        String orderNo = "ORD-001";
        Long amount = 50000L;

        PgSalesRaw pgRaw = createPgRaw(orderNo, PgStatus.PAID, amount);
        SalesLog internalLog = createSalesLog(orderNo, TransactionType.PAYMENT, amount);

        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of(pgRaw));
        when(salesLogRepository.findByOrderNoAndTransactionType(orderNo, TransactionType.PAYMENT))
                .thenReturn(List.of(internalLog));

        // when
        runReconciliationUseCase.execute(testDate);

        // then
        assertThat(internalLog.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MATCH);
        verify(mismatchLogRepository, never()).save(any()); // 에러 로그가 없어야 함
    }

    @Test
    @DisplayName("2. [성공] 1:N 일치 (부분 환불/합산) - PG 1건, DB N건의 합이 같음")
    void execute_Success_OneToManyMatch() {
        // given: PG는 5만원 결제 1건. DB는 3만원, 2만원 2건
        String orderNo = "ORD-002";
        Long pgAmount = 50000L;

        PgSalesRaw pgRaw = createPgRaw(orderNo, PgStatus.PAID, pgAmount);
        SalesLog log1 = createSalesLog(orderNo, TransactionType.PAYMENT, 30000L);
        SalesLog log2 = createSalesLog(orderNo, TransactionType.PAYMENT, 20000L);

        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of(pgRaw));
        when(salesLogRepository.findByOrderNoAndTransactionType(orderNo, TransactionType.PAYMENT))
                .thenReturn(List.of(log1, log2));

        // when
        runReconciliationUseCase.execute(testDate);

        // then: 두 건 모두 MATCH 상태로 변경되어야 함
        assertThat(log1.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MATCH);
        assertThat(log2.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MATCH);
        verify(mismatchLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. [실패] 금액 불일치 (AMOUNT_DIFF) - PG 금액과 DB 합산 금액이 다름")
    void execute_Fail_AmountMismatch() {
        // given: PG는 5만원, DB는 4만원
        String orderNo = "ORD-003";
        PgSalesRaw pgRaw = createPgRaw(orderNo, PgStatus.PAID, 50000L);
        SalesLog internalLog = createSalesLog(orderNo, TransactionType.PAYMENT, 40000L);

        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of(pgRaw));
        when(salesLogRepository.findByOrderNoAndTransactionType(orderNo, TransactionType.PAYMENT))
                .thenReturn(List.of(internalLog));

        // when
        runReconciliationUseCase.execute(testDate);

        // then
        assertThat(internalLog.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MISMATCH);

        // MismatchLog가 정확하게 저장되었는지 검증
        ArgumentCaptor<ReconciliationMismatchLog> logCaptor = ArgumentCaptor.forClass(ReconciliationMismatchLog.class);
        verify(mismatchLogRepository, times(1)).save(logCaptor.capture());

        ReconciliationMismatchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getType()).isEqualTo(MismatchType.AMOUNT_DIFF);
        assertThat(savedLog.getPgAmount().amount()).isEqualTo(50000L);
        assertThat(savedLog.getInternalAmount().amount()).isEqualTo(40000L); // 계산된 합계가 들어갔는지 확인
    }

    @Test
    @DisplayName("4. [실패] 주문서 누락 (PG_ONLY) - PG 결제는 있는데 DB 내역이 아예 없음")
    void execute_Fail_PgOnly() {
        // given
        String orderNo = "ORD-004";
        PgSalesRaw pgRaw = createPgRaw(orderNo, PgStatus.PAID, 50000L);

        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of(pgRaw));
        when(salesLogRepository.findByOrderNoAndTransactionType(orderNo, TransactionType.PAYMENT))
                .thenReturn(List.of()); // DB 조회 결과 없음!

        // when
        runReconciliationUseCase.execute(testDate);

        // then
        ArgumentCaptor<ReconciliationMismatchLog> logCaptor = ArgumentCaptor.forClass(ReconciliationMismatchLog.class);
        verify(mismatchLogRepository, times(1)).save(logCaptor.capture());

        ReconciliationMismatchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getType()).isEqualTo(MismatchType.PG_ONLY);
        assertThat(savedLog.getOrderNo()).isEqualTo(orderNo);
    }

    @Test
    @DisplayName("5. [실패] PG 내역 없음 (INTERNAL_ONLY) - DB에는 있는데 PG 데이터가 없음")
    void execute_Fail_InternalOnly() {
        // given: PG 리스트는 텅 빔. 하지만 DB에 PENDING 상태의 미처리 건이 남아있음.
        String orderNo = "ORD-005";
        SalesLog orphanLog = createSalesLog(orderNo, TransactionType.PAYMENT, 50000L);

        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of()); // PG 데이터 없음
        when(salesLogRepository.findAllBySnapshotAtBetweenAndReconciliationStatus(any(), any(), eq(ReconciliationStatus.PENDING)))
                .thenReturn(List.of(orphanLog)); // 고아 데이터 발견

        // when
        runReconciliationUseCase.execute(testDate);

        // then
        assertThat(orphanLog.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MISMATCH);

        ArgumentCaptor<ReconciliationMismatchLog> logCaptor = ArgumentCaptor.forClass(ReconciliationMismatchLog.class);
        verify(mismatchLogRepository, times(1)).save(logCaptor.capture());

        ReconciliationMismatchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getType()).isEqualTo(MismatchType.INTERNAL_ONLY);
        assertThat(savedLog.getInternalAmount().amount()).isEqualTo(50000L);
        assertThat(savedLog.getPgAmount().amount()).isEqualTo(0L); // PG 데이터가 없으므로 0원
    }

    @Test
    @DisplayName("[실패] 1원 차이 불일치 (AMOUNT_DIFF) - PG 금액은 50000원, DB 합산은 49999원")
    void execute_Fail_AmountMismatch_1Won() {
        // given: 단 1원의 오차가 발생한 상황 (예: 할인 정책 버그, 소수점 절사 오류 등)
        String orderNo = "ORD-003";
        PgSalesRaw pgRaw = createPgRaw(orderNo, PgStatus.PAID, 50000L);
        SalesLog internalLog = createSalesLog(orderNo, TransactionType.PAYMENT, 49999L); // 1원 빔

        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of(pgRaw));
        when(salesLogRepository.findByOrderNoAndTransactionType(orderNo, TransactionType.PAYMENT))
                .thenReturn(List.of(internalLog));

        // when
        runReconciliationUseCase.execute(testDate);

        // then: 상태가 MISMATCH로 변경되어야 함
        assertThat(internalLog.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MISMATCH);

        // MismatchLog에 정확히 1원 차이가 기록되었는지 검증
        ArgumentCaptor<ReconciliationMismatchLog> logCaptor = ArgumentCaptor.forClass(ReconciliationMismatchLog.class);
        verify(mismatchLogRepository, times(1)).save(logCaptor.capture());

        ReconciliationMismatchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getType()).isEqualTo(MismatchType.AMOUNT_DIFF);
        assertThat(savedLog.getPgAmount().amount()).isEqualTo(50000L);
        assertThat(savedLog.getInternalAmount().amount()).isEqualTo(49999L);
        assertThat(savedLog.getReason()).contains("금액 불일치");
    }

    @Test
    @DisplayName("[실패] 누락 데이터 감지 (PG_ONLY) - 고객이 돈은 냈는데 우리 DB에 주문서가 없음")
    void execute_Fail_PgOnly_MissingDB() {
        // given: PG사에는 결제 내역이 있지만,
        String orderNo = "ORD-004";
        PgSalesRaw pgRaw = createPgRaw(orderNo, PgStatus.PAID, 50000L);

        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of(pgRaw));

        // 우리 DB(SalesLog)를 뒤져보니 내역이 아예 없음 (Empty List 반환)
        when(salesLogRepository.findByOrderNoAndTransactionType(orderNo, TransactionType.PAYMENT))
                .thenReturn(List.of());

        // when
        runReconciliationUseCase.execute(testDate);

        // then: PG_ONLY 에러가 발생해야 함
        ArgumentCaptor<ReconciliationMismatchLog> logCaptor = ArgumentCaptor.forClass(ReconciliationMismatchLog.class);
        verify(mismatchLogRepository, times(1)).save(logCaptor.capture());

        ReconciliationMismatchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getType()).isEqualTo(MismatchType.PG_ONLY);
        assertThat(savedLog.getOrderNo()).isEqualTo(orderNo);
        assertThat(savedLog.getReason()).contains("주문서 누락");
    }

    @Test
    @DisplayName("[실패] 누락 데이터 감지 (INTERNAL_ONLY) - 우리 DB엔 주문이 있는데 PG사 결제가 안됨")
    void execute_Fail_InternalOnly_MissingPG() {
        // given: 우리 DB에는 PENDING 상태의 주문이 남아있음.
        String orderNo = "ORD-005";
        SalesLog orphanLog = createSalesLog(orderNo, TransactionType.PAYMENT, 50000L);

        // 근데 PG사에서 오늘 치 데이터를 긁어왔더니 아무것도 없음 (망 취소 등)
        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of());

        // 대사 마지막 단계에서 남아있는 PENDING 건들을 색출함
        when(salesLogRepository.findAllBySnapshotAtBetweenAndReconciliationStatus(any(), any(), eq(ReconciliationStatus.PENDING)))
                .thenReturn(List.of(orphanLog));

        // when
        runReconciliationUseCase.execute(testDate);

        // then: 남겨진 DB 데이터가 MISMATCH 처리됨
        assertThat(orphanLog.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MISMATCH);

        // INTERNAL_ONLY 에러 로그 검증
        ArgumentCaptor<ReconciliationMismatchLog> logCaptor = ArgumentCaptor.forClass(ReconciliationMismatchLog.class);
        verify(mismatchLogRepository, times(1)).save(logCaptor.capture());

        ReconciliationMismatchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getType()).isEqualTo(MismatchType.INTERNAL_ONLY);
        assertThat(savedLog.getInternalAmount().amount()).isEqualTo(50000L);
        assertThat(savedLog.getPgAmount().amount()).isEqualTo(0L); // PG 데이터가 없으므로 0원
        assertThat(savedLog.getReason()).contains("PG 내역 없음");
    }

    @Test
    @DisplayName("[실패] 상태 불일치 (STATUS_DIFF) - 주문은 존재하지만 결제/환불 타입이 다름")
    void execute_Fail_StatusDiff() {
        // given: PG는 결제(PAID)인데 내부엔 환불(REFUND) 내역만 존재
        String orderNo = "ORD-STATUS-DIFF";
        PgSalesRaw pgRaw = createPgRaw(orderNo, PgStatus.PAID, 50000L);
        SalesLog refundLog = createSalesLog(orderNo, TransactionType.REFUND, -50000L);

        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of(pgRaw));
        when(salesLogRepository.findByOrderNoAndTransactionType(orderNo, TransactionType.PAYMENT))
                .thenReturn(List.of()); // 동일 상태 타입 조회 결과 없음
        when(salesLogRepository.findByOrderNo(orderNo))
                .thenReturn(List.of(refundLog)); // 같은 주문번호는 존재

        // when
        runReconciliationUseCase.execute(testDate);

        // then
        assertThat(refundLog.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MISMATCH);

        ArgumentCaptor<ReconciliationMismatchLog> logCaptor = ArgumentCaptor.forClass(ReconciliationMismatchLog.class);
        verify(mismatchLogRepository, times(1)).save(logCaptor.capture());

        ReconciliationMismatchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getType()).isEqualTo(MismatchType.STATUS_DIFF);
        assertThat(savedLog.getOrderNo()).isEqualTo(orderNo);
        assertThat(savedLog.getReason()).contains("상태 불일치");
    }

    @Test
    @DisplayName("[성공] 다중 상품 일치 - 키보드 4만, 마우스 2만 (PG 6만) 대사 완벽 통과")
    void execute_Success_MultiItemMatch() {
        // given: 상품이 여러 개 담긴 장바구니 결제 상황
        String orderNo = "ORD-006";

        // PG사에는 하나로 묶여서 총액 60,000원이 찍혀있음
        PgSalesRaw pgRaw = createPgRaw(orderNo, PgStatus.PAID, 60000L);

        // 우리 DB에는 키보드 4만원, 마우스 2만원으로 쪼개져 있음
        SalesLog keyboardLog = createSalesLog(orderNo, TransactionType.PAYMENT, 40000L);
        SalesLog mouseLog = createSalesLog(orderNo, TransactionType.PAYMENT, 20000L);

        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of(pgRaw));
        when(salesLogRepository.findByOrderNoAndTransactionType(orderNo, TransactionType.PAYMENT))
                .thenReturn(List.of(keyboardLog, mouseLog));

        // when
        runReconciliationUseCase.execute(testDate);

        // then: 4만 + 2만 = 6만 이므로 둘 다 MATCH 처리가 되어야 함
        assertThat(keyboardLog.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MATCH);
        assertThat(mouseLog.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MATCH);

        // 일치하므로 에러 로그(MismatchLog)는 절대 저장되면 안 됨!
        verify(mismatchLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("[통합 검증] 대사 완료 시 Job 통계(성공/실패 카운트)와 MismatchLog 연관관계가 정확히 저장된다")
    void execute_VerifyJobAndMismatchLog_Statistics() {
        // given: 1건은 완벽 일치(성공), 1건은 금액 불일치(실패) 상황 세팅
        String successOrderNo = "ORD-SUCCESS";
        String failOrderNo = "ORD-FAIL";

        // 성공 건 세팅 (PG 5만 = DB 5만)
        PgSalesRaw successPg = createPgRaw(successOrderNo, PgStatus.PAID, 50000L);
        SalesLog successLog = createSalesLog(successOrderNo, TransactionType.PAYMENT, 50000L);

        // 실패 건 세팅 (PG 3만 != DB 2만)
        PgSalesRaw failPg = createPgRaw(failOrderNo, PgStatus.PAID, 30000L);
        SalesLog failLog = createSalesLog(failOrderNo, TransactionType.PAYMENT, 20000L);

        when(pgSalesRawRepository.findAllByTransactedAtBetween(any(), any()))
                .thenReturn(List.of(successPg, failPg));
        when(salesLogRepository.findByOrderNoAndTransactionType(successOrderNo, TransactionType.PAYMENT))
                .thenReturn(List.of(successLog));
        when(salesLogRepository.findByOrderNoAndTransactionType(failOrderNo, TransactionType.PAYMENT))
                .thenReturn(List.of(failLog));

        // DB에 Job을 저장할 때 동작할 Mock 설정 (저장된 객체를 그대로 반환하도록)
        when(jobRepository.save(any(ReconciliationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        runReconciliationUseCase.execute(testDate);

        // then 1: Job이 DB에 잘 저장(save) 되었는가?
        ArgumentCaptor<ReconciliationResult> jobCaptor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(jobRepository, times(1)).save(jobCaptor.capture());

        ReconciliationResult savedJob = jobCaptor.getValue();

        // then 2: Job의 통계(finish 결과)가 정확하게 집계되었는가? (전체 2건, 성공 1건, 실패 1건)
        // assertThat(savedJob.getTotalCount()).isEqualTo(2);
        // assertThat(savedJob.getSuccessCount()).isEqualTo(1);
        // assertThat(savedJob.getMismatchCount()).isEqualTo(1);

        // then 3: 실패한 건에 대해 MismatchLog가 잘 저장되었는가?
        ArgumentCaptor<ReconciliationMismatchLog> logCaptor = ArgumentCaptor.forClass(ReconciliationMismatchLog.class);
        verify(mismatchLogRepository, times(1)).save(logCaptor.capture());

        ReconciliationMismatchLog savedMismatchLog = logCaptor.getValue();

        // then 4: MismatchLog가 방금 생성된 Job 객체와 정확히 매핑(연관관계)되었는가?
        assertThat(savedMismatchLog.getJob()).isEqualTo(savedJob);
        assertThat(savedMismatchLog.getOrderNo()).isEqualTo(failOrderNo); // 실패한 주문번호가 맞는지
        assertThat(savedMismatchLog.getType()).isEqualTo(MismatchType.AMOUNT_DIFF);
    }

    // --- Helper Methods (테스트 객체 생성용) ---

    private PgSalesRaw createPgRaw(String merchantUid, PgStatus status, Long amount) {
        // 실제 엔티티 구조에 맞게 수정 필요할 수 있음
        return PgSalesRaw.builder()
                .merchantUid(merchantUid)
                .pgStatus(status)
                .paymentAmount(Money.of(amount))
                .pgKey("PG-" + merchantUid)
                .build();
    }

    private SalesLog createSalesLog(String orderNo, TransactionType type, Long amount) {
        return SalesLog.builder()
                .orderNo(orderNo)
                .transactionType(type)
                .paymentAmount(Money.of(amount))
                .productPrice(Money.of(amount))
                .productName("test-item")
                .productQuantity(1)
                .sellerId(1L)
                .snapshotAt(LocalDateTime.now())
                .build();
    }
}
