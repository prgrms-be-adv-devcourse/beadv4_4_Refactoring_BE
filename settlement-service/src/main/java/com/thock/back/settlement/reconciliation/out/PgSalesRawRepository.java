package com.thock.back.settlement.reconciliation.out;

import com.thock.back.settlement.reconciliation.domain.PgSalesRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PgSalesRawRepository extends JpaRepository<PgSalesRaw, Long> {
    List<PgSalesRaw> findAllByTransactedAtBetween(LocalDateTime start, LocalDateTime end);
}
