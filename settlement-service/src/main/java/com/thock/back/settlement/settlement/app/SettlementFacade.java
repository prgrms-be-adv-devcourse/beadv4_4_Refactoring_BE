package com.thock.back.settlement.settlement.app;

import com.thock.back.settlement.settlement.app.service.SettlementQueryService;
import com.thock.back.settlement.settlement.app.useCase.RunDailySettlementUseCase;
import com.thock.back.settlement.settlement.app.useCase.RunMonthlySettlementUseCase;
import com.thock.back.settlement.settlement.in.dto.DailySettlementItemsResponseItem;
import com.thock.back.settlement.settlement.in.dto.MonthlySettlementSummaryResponseItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementFacade {

    private final RunDailySettlementUseCase runDailySettlementUseCase;
    private final RunMonthlySettlementUseCase runMonthlySettlementUseCase;
    private final SettlementQueryService settlementQueryService;

    // 일별 정산 실행
    public void runDailySettlement(LocalDate targetDate) {
        runDailySettlementUseCase.execute(targetDate);
    }

    // 월별 정산 실행
    public void runMonthlySettlement(YearMonth targetMonth) {
        runMonthlySettlementUseCase.execute(targetMonth);
    }

    // 월별 정산 내역서
    public List<MonthlySettlementSummaryResponseItem> getMonthlySummary(Long sellerId, YearMonth targetMonth) {
        return settlementQueryService.getMonthlySummary(sellerId, targetMonth);
    }

    // 일별 세부 내역서 실행
    public List<DailySettlementItemsResponseItem> getDailyItems(Long sellerId, LocalDate targetDate) {
        return settlementQueryService.getDailyItems(sellerId, targetDate);
    }
}
