package com.thock.back.settlement.shared.slack.event;

public record MonthlySettlementFailedAlertEvent(
        Long sellerId,
        String targetYearMonth,
        int retryCount,
        String reason
) {
}
