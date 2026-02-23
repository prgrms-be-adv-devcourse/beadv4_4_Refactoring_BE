package com.thock.back.settlement.settlement.in.controller;

import com.thock.back.settlement.reconciliation.app.service.ManualReconciliationScenarioService;
import com.thock.back.settlement.settlement.app.service.SettlementBatchLauncher;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settlements/manual")
public class SettlementManualController {

    private final ManualReconciliationScenarioService manualReconciliationScenarioService;
    private final SettlementBatchLauncher settlementBatchLauncher;

    // 수동 일별 정산
    @PostMapping("/daily-settlements/executions")
    public Map<String, Object> executeDailySettlement(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate == null ? LocalDate.now() : targetDate;
        manualReconciliationScenarioService.runDailySettlement(date);

        return Map.of(
                "executionType", "daily-settlement",
                "targetDate", date,
                "status", "done"
        );
    }

    // 수동 월별 정산
    @PostMapping("/monthly-settlements/executions")
    public Map<String, Object> executeMonthlySettlement(
            @RequestParam(required = false) String targetMonth
    ) {
        YearMonth month = targetMonth == null ? YearMonth.now() : YearMonth.parse(targetMonth);
        manualReconciliationScenarioService.runMonthlySettlement(month);

        return Map.of(
                "executionType", "monthly-settlement",
                "targetMonth", month.toString(),
                "status", "done"
        );
    }

    // 수동 일별정산 배치
    @PostMapping("/daily-settlement-batches/executions")
    public Map<String, Object> executeDailySettlementBatch(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate == null ? LocalDate.now() : targetDate;
        SettlementBatchLauncher.BatchRunResult result = settlementBatchLauncher.runDaily(date);

        return Map.of(
                "executionType", "daily-settlement-batch",
                "targetDate", date,
                "batchId", result.batchId(),
                "executionId", result.executionId(),
                "status", result.status()
        );
    }

    // 수동 월별정산 배치
    @PostMapping("/monthly-settlement-batches/executions")
    public Map<String, Object> executeMonthlySettlementBatch(
            @RequestParam(required = false) String targetMonth
    ) {
        YearMonth month = targetMonth == null ? YearMonth.now() : YearMonth.parse(targetMonth);
        SettlementBatchLauncher.BatchRunResult result = settlementBatchLauncher.runMonthly(month);

        return Map.of(
                "executionType", "monthly-settlement-batch",
                "targetMonth", month.toString(),
                "batchId", result.batchId(),
                "executionId", result.executionId(),
                "status", result.status()
        );
    }
}
