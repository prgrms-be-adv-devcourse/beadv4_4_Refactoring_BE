package com.thock.back.settlement.settlement.in.dto;

import java.time.LocalDate;
import java.util.List;

// 일별 정산 세부내역서 응답 dto
public record DailySettlementItemsResponse(
        Long sellerId,
        LocalDate targetDate,
        List<DailySettlementItemsResponseItem> items
) {
    public static DailySettlementItemsResponse of(Long sellerId, LocalDate targetDate, List<DailySettlementItemsResponseItem> items) {
        return new DailySettlementItemsResponse(sellerId, targetDate, items);
    }
}
