package com.thock.back.settlement.reconciliation.out;

import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.domain.enums.ReconciliationStatus;
import com.thock.back.settlement.shared.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesLogRepository extends JpaRepository<SalesLog, Long> {

    // --------- 대사 관련 리포지토리 ---------
    // PG사에는 상품들이 합쳐셔 결제 된 총 결제액만 나와있음(1), 우리 주문서는 나뉘어서 들어옴(N)
    // 부분확정, 부분환불 이슈 대비해 상품id까지 묶어서 조회
    Optional<SalesLog> findByOrderNoAndProductIdAndTransactionType(
            String orderNo,
            Long productId,
            TransactionType transactionType
    );
    List<SalesLog> findAllBySnapshotAtBetween(LocalDateTime start, LocalDateTime end);
    List<SalesLog> findByOrderNoAndTransactionType(String orderNo, TransactionType transactionType);
    List<SalesLog> findAllBySnapshotAtBetweenAndReconciliationStatus(LocalDateTime start, LocalDateTime end, ReconciliationStatus status);

    // -------- 내부 통신 관련 리포지토리 --------
    @Query(""" 
            SELECT s
            FROM SalesLog s
            WHERE s.reconciliationStatus = "MATCH"
            AND s.confirmedAt IS NOT NULL
            AND s.dailySettlementId IS NULL
    """)
    List<SalesLog> findSettlementCandidates();
}
