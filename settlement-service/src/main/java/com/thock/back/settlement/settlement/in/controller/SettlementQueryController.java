package com.thock.back.settlement.settlement.in.controller;

import com.thock.back.settlement.settlement.app.SettlementFacade;
import com.thock.back.settlement.settlement.in.dto.DailySettlementItemsResponse;
import com.thock.back.settlement.settlement.in.dto.MonthlySettlementSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
// 정산 관련 판매자 페이지에 들어갈 기능
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settlements/settlement/query")
public class SettlementQueryController {

    private final SettlementFacade settlementFacade;

    // 월별 정산 내역
    @GetMapping("/monthly")
    public MonthlySettlementSummaryResponse getMonthlySummary(
            @RequestParam Long sellerId,
            @RequestParam String targetMonth
    ) {
        YearMonth month = YearMonth.parse(targetMonth);
        return MonthlySettlementSummaryResponse.of(
                sellerId,
                month,
                settlementFacade.getMonthlySummary(sellerId, month)
        );
    }

    // 일별 정산 세부내역서
    @GetMapping("/daily-items")
    public DailySettlementItemsResponse getDailyItems(
            @RequestParam Long sellerId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        return DailySettlementItemsResponse.of(
                sellerId,
                targetDate,
                settlementFacade.getDailyItems(sellerId, targetDate)
        );
    }
}
