package com.thock.back.settlement.settlement.out;

import com.thock.back.settlement.settlement.domain.MonthlySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlySettlementRepository extends JpaRepository <MonthlySettlement, Long> {
    List<MonthlySettlement> findBySellerIdAndTargetYearMonth(Long sellerId, String targetYearMonth);
}
