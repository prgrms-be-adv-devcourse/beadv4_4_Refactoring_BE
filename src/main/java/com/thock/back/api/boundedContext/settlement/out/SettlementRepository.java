package com.thock.back.api.boundedContext.settlement.out;

import com.thock.back.api.boundedContext.settlement.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByMemberIdAndSalesDate(Long memberId, LocalDate salesDate);
}
