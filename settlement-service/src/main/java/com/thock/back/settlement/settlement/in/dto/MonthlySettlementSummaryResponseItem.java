package com.thock.back.settlement.settlement.in.dto;

import java.time.LocalDateTime;

public record MonthlySettlementSummaryResponseItem(
        Long monthlySettlementId,
        Long sellerId,
        String targetYearMonth,
        Long totalCount,
        Long totalPaymentAmount,
        Long totalFeeAmount,
        Long totalPayoutAmount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
