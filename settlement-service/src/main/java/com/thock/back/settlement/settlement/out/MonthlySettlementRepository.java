package com.thock.back.settlement.settlement.out;

import com.thock.back.settlement.settlement.domain.MonthlySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlySettlementRepository extends JpaRepository <MonthlySettlement, Long> {
    List<MonthlySettlement> findBySellerIdAndTargetYearMonth(Long sellerId, String targetYearMonth);
    Optional<MonthlySettlement> findFirstBySellerIdAndTargetYearMonth(Long sellerId, String targetYearMonth);
    boolean existsBySellerIdAndTargetYearMonth(Long sellerId, String targetYearMonth);
}
