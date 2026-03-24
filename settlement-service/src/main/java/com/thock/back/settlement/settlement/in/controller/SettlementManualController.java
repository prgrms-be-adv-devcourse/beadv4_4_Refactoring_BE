package com.thock.back.settlement.settlement.in.controller;

import com.thock.back.settlement.settlement.app.SettlementFacade;
import com.thock.back.settlement.settlement.app.service.SettlementBatchLauncher;
import com.thock.back.settlement.settlement.in.dto.SettlementManualExecutionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settlements/manual")
public class SettlementManualController {

    private final SettlementFacade settlementFacade;
    private final SettlementBatchLauncher settlementBatchLauncher;

    // 수동 일별 정산
    @PostMapping("/daily-settlements/executions")
    public SettlementManualExecutionResponse executeDailySettlement(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate == null ? LocalDate.now() : targetDate;
        settlementFacade.runDailySettlement(date);
        return SettlementManualExecutionResponse.daily(date);
    }

    // 수동 월별 정산
    @PostMapping("/monthly-settlements/executions")
    public SettlementManualExecutionResponse executeMonthlySettlement(
            @RequestParam(required = false) String targetMonth
    ) {
        YearMonth month = targetMonth == null ? YearMonth.now() : YearMonth.parse(targetMonth);
        settlementFacade.runMonthlySettlement(month);
        return SettlementManualExecutionResponse.monthly(month);
    }

    // 수동 일별정산 배치
    @PostMapping("/daily-settlement-batches/executions")
    public SettlementManualExecutionResponse executeDailySettlementBatch(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate == null ? LocalDate.now() : targetDate;
        SettlementBatchLauncher.BatchRunResult result = settlementBatchLauncher.runDaily(date);
        return SettlementManualExecutionResponse.dailyBatch(
                date,
                result.batchId(),
                result.executionId(),
                result.status()
        );
    }

    // 수동 월별정산 배치
    @PostMapping("/monthly-settlement-batches/executions")
    public SettlementManualExecutionResponse executeMonthlySettlementBatch(
            @RequestParam(required = false) String targetMonth
    ) {
        YearMonth month = targetMonth == null ? YearMonth.now() : YearMonth.parse(targetMonth);
        SettlementBatchLauncher.BatchRunResult result = settlementBatchLauncher.runMonthly(month);
        return SettlementManualExecutionResponse.monthlyBatch(
                month,
                result.batchId(),
                result.executionId(),
                result.status()
        );
    }
}
