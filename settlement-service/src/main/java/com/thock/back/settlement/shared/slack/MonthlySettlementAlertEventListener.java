package com.thock.back.settlement.shared.slack;

import com.thock.back.settlement.shared.slack.event.MonthlySettlementFailedAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlySettlementAlertEventListener {

    private final SlackNotifier slackNotifier;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(MonthlySettlementFailedAlertEvent event) {
        slackNotifier.notifyMonthlySettlementFailure(
                event.sellerId(),
                event.targetYearMonth(),
                event.retryCount(),
                event.reason()
        );
        log.info("월별 정산 최종 실패 Slack 알림 전송 완료. sellerId={}, targetYearMonth={}",
                event.sellerId(), event.targetYearMonth());
    }
}
