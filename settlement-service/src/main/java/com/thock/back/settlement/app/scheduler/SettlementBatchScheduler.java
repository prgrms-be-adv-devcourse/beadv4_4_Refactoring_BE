package com.thock.back.settlement.app.scheduler;

import com.thock.back.settlement.reconciliation.app.service.ReconciliationBatchLauncher;
import com.thock.back.settlement.settlement.app.service.SettlementBatchLauncher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementBatchScheduler {

    private final ReconciliationBatchLauncher reconciliationBatchLauncher;
    private final SettlementBatchLauncher settlementBatchLauncher;

    // 매일 오전 1시: 전일 기준 대사 실행
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void runDailyReconciliationAt1am() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        log.info("[Scheduler] 대사 배치 시작 targetDate={}", targetDate);
        reconciliationBatchLauncher.run(targetDate);
    }

    // 매일 오전 2시: 전일 기준 일별 정산 실행
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void runDailySettlementAt2am() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        log.info("[Scheduler] 일별 정산 배치 시작 targetDate={}", targetDate);
        settlementBatchLauncher.runDaily(targetDate);
    }

    // 매일 오전 3시: 전월 기준 월별 정산 실행
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void runMonthlySettlementAt3am() {
        YearMonth targetMonth = YearMonth.now().minusMonths(1);
        log.info("[Scheduler] 월별 정산 배치 시작 targetMonth={}", targetMonth);
        settlementBatchLauncher.runMonthly(targetMonth);
    }
}
