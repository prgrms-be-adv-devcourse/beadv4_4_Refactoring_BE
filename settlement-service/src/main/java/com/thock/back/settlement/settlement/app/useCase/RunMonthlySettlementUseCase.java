package com.thock.back.settlement.settlement.app.useCase;

import com.thock.back.settlement.settlement.domain.DailySettlement;
import com.thock.back.settlement.settlement.domain.MonthlySettlement;
import com.thock.back.settlement.shared.money.Money;
import com.thock.back.settlement.settlement.out.DailySettlementRepository;
import com.thock.back.settlement.settlement.out.MonthlySettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunMonthlySettlementUseCase {

    private final DailySettlementRepository dailySettlementRepository;
    private final MonthlySettlementRepository monthlySettlementRepository;
    // TODO : 현재 일별 정산 데이터(1일~31일)까지 모두 긁어 오기 때문에, 데이터가 많으면 메모리가 부족할 수 있음
    // TODO : 따라서 판매자 목록을 조회 후, 루프를 도는 것이 안전
    // TODO : 대용량 처리를 위해 페이징 처리(offest, cursor) 고도화 예정
    @Transactional
    public void execute(YearMonth targetMonth) {
        log.info("=========[월별 정산 시작] 기준월: {} =========", targetMonth);

        // 1. 조회 기간 설정 (예: 2026-02-01 ~ 2026-02-28)
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        // 2. 이번 달에 정산된 '모든' 일별 정산 데이터 조회 (일단 다 가져옴)
        List<DailySettlement> allDailySettlements = dailySettlementRepository.findAllByTargetDateBetween(startDate, endDate);

        if (allDailySettlements.isEmpty()) {
            log.info("정산할 일별 데이터가 없습니다.");
            return;
        }

        // 3. 판매자별로 그룹핑 (Map<판매자ID, 리스트>)
        Map<Long, List<DailySettlement>> groups = allDailySettlements.stream()
                .collect(Collectors.groupingBy(DailySettlement::getSellerId));

        // 4. 각 판매자별로 월별 정산서 생성
        for (Long sellerId : groups.keySet()) {
            List<DailySettlement> dailyItems = groups.get(sellerId);

            // 4-1. 합계 계산 (Sum)
            long totalCount = dailyItems.size(); // 일별 정산 건수
            long totalPayment = dailyItems.stream().mapToLong(d -> d.getPaymentAmount().amount()).sum();
            long totalFee = dailyItems.stream().mapToLong(d -> d.getFeeAmount().amount()).sum();
            long totalPayout = dailyItems.stream().mapToLong(d -> d.getSettlementAmount().amount()).sum();

            // 4-2. 월별 정산서 엔티티 생성
            MonthlySettlement monthlySettlement = MonthlySettlement.builder()
                    .sellerId(sellerId)
                    .targetYearMonth(targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM")))
                    .totalCount(totalCount)
                    .totalPaymentAmount(Money.of(totalPayment))
                    .totalFeeAmount(Money.of(totalFee))
                    .totalPayoutAmount(Money.of(totalPayout))
                    .build();

            // 5. 저장
            monthlySettlementRepository.save(monthlySettlement);
        }

        log.info("=========[월별 정산 종료] 처리된 판매자 수: {} =========", groups.size());
    }
}
