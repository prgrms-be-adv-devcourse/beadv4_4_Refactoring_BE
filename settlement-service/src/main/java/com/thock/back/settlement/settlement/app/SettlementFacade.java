package com.thock.back.settlement.settlement.app;

import com.thock.back.settlement.settlement.app.service.SettlementQueryService;
import com.thock.back.settlement.settlement.app.useCase.RunDailySettlementUseCase;
import com.thock.back.settlement.settlement.app.useCase.RunMonthlySettlementUseCase;
import com.thock.back.settlement.settlement.in.dto.DailySettlementItemView;
import com.thock.back.settlement.settlement.in.dto.MonthlySettlementView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementFacade {

    private final RunDailySettlementUseCase runDailySettlementUseCase;
    private final RunMonthlySettlementUseCase runMonthlySettlementUseCase;
    private final SettlementQueryService settlementQueryService;

    @Transactional
    public void runDaily(LocalDate targetDate) {
        runDailySettlementUseCase.execute(targetDate);
    }

    @Transactional
    public void runMonthly(YearMonth targetMonth) {
        runMonthlySettlementUseCase.execute(targetMonth);
    }

    @Transactional(readOnly = true)
    public List<MonthlySettlementView> getMonthlySummary(Long sellerId, YearMonth targetMonth) {
        return settlementQueryService.getMonthlySummary(sellerId, targetMonth);
    }

    @Transactional(readOnly = true)
    public List<DailySettlementItemView> getDailyItems(Long sellerId, LocalDate targetDate) {
        return settlementQueryService.getDailyItems(sellerId, targetDate);
    }
}
