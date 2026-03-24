package com.thock.back.settlement.reconciliation.in.adapter;

import com.thock.back.settlement.reconciliation.app.port.SettlementCandidate;
import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.domain.enums.ReconciliationStatus;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import com.thock.back.settlement.shared.enums.TransactionType;
import com.thock.back.settlement.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // 실제 스프링 컨테이너를 띄워서 테스트 (DB 연결됨)
@Transactional // 테스트 끝나면 데이터 롤백 (DB 깨끗하게 유지)
class SettlementTargetAdapterTest {

    @Autowired
    private SettlementTargetAdapter adapter; // 테스트 대상

    @Autowired
    private SalesLogRepository salesLogRepository; // 데이터 준비용

    @Test
    @DisplayName("대사가 성공(MATCH)했고, 아직 정산되지 않은 데이터만 가져와야 한다.")
    void getCandidates_success() {
        // given (상황 만들기)
        // 1. 가져와야 하는 놈 (MATCH, 정산 안 됨)
        SalesLog target = SalesLog.builder()
                .orderNo("ORD-TARGET-001")
                .sellerId(1L)
                .reconciliationStatus(ReconciliationStatus.MATCH) // 대사 성공
                .dailySettlementId(null) // 정산 안 됨
                .paymentAmount(Money.of(10000L))
                .productId(100L)
                .productName("키보드")
                .productQuantity(1)
                .productPrice(Money.of(10000L))
                .transactionType(TransactionType.PAYMENT)
                .snapshotAt(LocalDateTime.now())
                .confirmedAt(LocalDateTime.now())
                .build();

        // 2. 대사 실패한 놈 (MISMATCH)
        SalesLog failLog = SalesLog.builder()
                .orderNo("ORD-FAIL-001")
                .sellerId(1L)
                .reconciliationStatus(ReconciliationStatus.MISMATCH) // 실패!
                .dailySettlementId(null)
                .paymentAmount(Money.of(20000L))
                .productId(101L)
                .productName("마우스")
                .productQuantity(1)
                .productPrice(Money.of(20000L))
                .transactionType(TransactionType.PAYMENT)
                .snapshotAt(LocalDateTime.now())
                .build();

        // 3. 이미 정산된 놈 (dailySettlementId 존재)
        SalesLog settledLog = SalesLog.builder()
                .orderNo("ORD-SETTLED-001")
                .sellerId(1L)
                .reconciliationStatus(ReconciliationStatus.MATCH)
                .dailySettlementId(999L) // 이미 정산됨!
                .paymentAmount(Money.of(30000L))
                .productId(102L)
                .productName("모니터")
                .productQuantity(1)
                .productPrice(Money.of(30000L))
                .transactionType(TransactionType.PAYMENT)
                .snapshotAt(LocalDateTime.now())
                .build();

        salesLogRepository.saveAll(List.of(target, failLog, settledLog));

        // when (실행)
        List<SettlementCandidate> candidates = adapter.getCandidates();

        // then (검증)
        assertThat(candidates).hasSize(1); // 딱 1개만 와야 함

        SettlementCandidate result = candidates.get(0);
        assertThat(result.salesLogId()).isEqualTo(target.getId()); // 그게 1번 놈이어야 함
        assertThat(result.paymentAmount()).isEqualTo(10000L); // 금액도 맞는지 확인
    }
}
