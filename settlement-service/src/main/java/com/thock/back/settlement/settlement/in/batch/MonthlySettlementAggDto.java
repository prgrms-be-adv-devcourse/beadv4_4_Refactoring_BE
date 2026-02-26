package com.thock.back.settlement.settlement.in.batch;

public record MonthlySettlementAggDto(
        Long sellerId,
        Long totalCount,
        Long totalPaymentAmount,
        Long totalFeeAmount,
        Long totalPayoutAmount
) {
}
