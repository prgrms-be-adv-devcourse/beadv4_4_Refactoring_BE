package com.thock.back.settlement.reconciliation.out;

import com.thock.back.settlement.reconciliation.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
