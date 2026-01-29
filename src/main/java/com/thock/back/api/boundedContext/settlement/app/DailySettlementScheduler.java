package com.thock.back.api.boundedContext.settlement.app;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DailySettlementScheduler {

    private final SettlementFacade settlementFacade;

    @Scheduled(cron = "0 0 1 * * *")
    public void runDailySettlement() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        settlementFacade.runDailySettlement(yesterday);
    }
}