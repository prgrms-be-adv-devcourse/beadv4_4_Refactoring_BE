package com.thock.back.settlement.reconciliation.out;

import com.thock.back.settlement.reconciliation.domain.ReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationJobRepository extends JpaRepository<ReconciliationResult, Long> {
}
