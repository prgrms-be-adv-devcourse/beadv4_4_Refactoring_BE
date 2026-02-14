package com.thock.back.settlement.settlement.in.dto;

import java.time.LocalDate;

public record DailySettlementItemView(
        Long dailySettlementId,
        Long sellerId,
        LocalDate targetDate,
        Long dailySettlementItemId,
        Long productId,
        String productName,
        int finalQuantity,
        Long finalAmount,
        String dailySettlementStatus
) {
}
