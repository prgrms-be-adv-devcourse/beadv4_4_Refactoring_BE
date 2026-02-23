package com.thock.back.settlement.settlement.in.dto;

import java.time.YearMonth;
import java.util.List;

// 월별 정산 요약서
public record MonthlySettlementSummaryResponse(
        Long sellerId,
        String targetMonth,
        List<MonthlySettlementSummaryResponseItem> items
) {
    public static MonthlySettlementSummaryResponse of(Long sellerId, YearMonth targetMonth, List<MonthlySettlementSummaryResponseItem> items) {
        return new MonthlySettlementSummaryResponse(sellerId, targetMonth.toString(), items);
    }
}
