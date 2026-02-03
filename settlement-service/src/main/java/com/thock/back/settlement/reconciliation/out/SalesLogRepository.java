package com.thock.back.settlement.reconciliation.out;

import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.domain.enums.SettlementStatus;
import com.thock.back.settlement.shared.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesLogRepository extends JpaRepository<SalesLog, Long> {
    List<SalesLog> findAllBySettlementStatus(SettlementStatus settlementStatus);
    Optional<SalesLog> findByOrderNoAndTransactionType(String orderNo, TransactionType transactionType);
}
