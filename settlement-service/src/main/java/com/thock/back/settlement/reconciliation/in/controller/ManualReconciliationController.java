package com.thock.back.settlement.reconciliation.in.controller;

import com.thock.back.settlement.reconciliation.app.service.ManualReconciliationScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;


// 1차 통합테스트를 위한 수동 트키러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance/reconciliation/manual")
public class ManualReconciliationController {

    private final ManualReconciliationScenarioService manualReconciliationScenarioService;

    // SalesLog 기반 PG 데이터 생성 (대사 일치 과정 확인용)
    @PostMapping("/generate-pg-from-saleslog")
    public ManualReconciliationScenarioService.GeneratePgResult generatePgFromSalesLog(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate == null ? LocalDate.now() : targetDate;
        return manualReconciliationScenarioService.generatePgRawFromSalesLogs(date);
    }

    // 일별 대사 수동 컨트롤러
    @PostMapping("/run-reconciliation")
    public Map<String, Object> runReconciliation(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate == null ? LocalDate.now() : targetDate;
        manualReconciliationScenarioService.runReconciliation(date);
        return Map.of("step", "reconciliation", "targetDate", date, "status", "done");
    }

    // 일별 정산 수동 컨트롤러
    @PostMapping("/run-daily")
    public Map<String, Object> runDaily(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate == null ? LocalDate.now() : targetDate;
        manualReconciliationScenarioService.runDailySettlement(date);
        return Map.of("step", "daily", "targetDate", date, "status", "done");
    }

    // 월별 정산 수동 컨트롤러
    @PostMapping("/run-monthly")
    public Map<String, Object> runMonthly(
            @RequestParam(required = false) String targetMonth
    ) {
        YearMonth month = targetMonth == null ? YearMonth.now() : YearMonth.parse(targetMonth);
        manualReconciliationScenarioService.runMonthlySettlement(month);
        return Map.of("step", "monthly", "targetMonth", month.toString(), "status", "done");
    }

    // PG파일 생성 - 대사 -일별정산 - 월별정산 수동 총 진행
    @PostMapping("/run-all")
    public ManualReconciliationScenarioService.RunAllResult runAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate == null ? LocalDate.now() : targetDate;
        return manualReconciliationScenarioService.runAll(date);
    }
}
