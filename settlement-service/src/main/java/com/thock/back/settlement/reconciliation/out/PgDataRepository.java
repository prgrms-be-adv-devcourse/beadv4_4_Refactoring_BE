package com.thock.back.settlement.reconciliation.out;

import com.thock.back.settlement.reconciliation.domain.PgSalesRaw;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PgDataRepository extends JpaRepository<PgSalesRaw, Long> {
}
