package com.thock.back.settlement.settlement.in.batch;

public record MonthlySettlementWriteModel(
        Long sellerId,
        Long totalCount,
        Long totalPaymentAmount,
        Long totalFeeAmount,
        Long totalPayoutAmount
) {
}
