package com.thock.back.settlement.settlement.out;

import com.thock.back.settlement.settlement.domain.DailySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySettlementRepository extends JpaRepository<DailySettlement, Long> {
}
