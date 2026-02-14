package com.thock.back.settlement.settlement.in.controller;

import com.thock.back.settlement.settlement.app.service.SettlementQueryService;
import com.thock.back.settlement.settlement.in.dto.DailySettlementItemView;
import com.thock.back.settlement.settlement.in.dto.MonthlySettlementView;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;


// 정산 관련 판매자 페이지에 들어갈 기능
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance/settlement/query")
public class SettlementQueryController {

    private final SettlementQueryService settlementQueryService;

    // 월별 정산 내역
    @GetMapping("/monthly")
    public List<MonthlySettlementView> getMonthlySummary(
            @RequestParam Long sellerId,
            @RequestParam String targetMonth
    ) {
        return settlementQueryService.getMonthlySummary(sellerId, YearMonth.parse(targetMonth));
    }

    // 일별 정산 세부내역서
    @GetMapping("/daily-items")
    public List<DailySettlementItemView> getDailyItems(
            @RequestParam Long sellerId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        return settlementQueryService.getDailyItems(sellerId, targetDate);
    }
}
