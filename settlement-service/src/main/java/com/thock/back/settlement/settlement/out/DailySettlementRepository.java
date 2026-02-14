package com.thock.back.settlement.settlement.out;

import com.thock.back.settlement.settlement.domain.DailySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailySettlementRepository extends JpaRepository<DailySettlement, Long> {

    //일별 판매 상세 목록
    List<DailySettlement> findBySellerIdAndTargetDate(Long sellerId, LocalDate targetDate);

    //일별 판매
    List<DailySettlement> findAllByTargetDateBetween(LocalDate startDate, LocalDate endDate);

}
