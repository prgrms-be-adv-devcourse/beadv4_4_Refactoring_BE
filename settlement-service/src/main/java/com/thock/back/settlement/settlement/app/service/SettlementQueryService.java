package com.thock.back.settlement.settlement.app.service;
import com.thock.back.settlement.settlement.domain.DailySettlement;
import com.thock.back.settlement.settlement.domain.DailySettlementItem;
import com.thock.back.settlement.settlement.domain.MonthlySettlement;
import com.thock.back.settlement.settlement.in.dto.DailySettlementItemView;
import com.thock.back.settlement.settlement.in.dto.MonthlySettlementView;
import com.thock.back.settlement.settlement.out.DailySettlementRepository;
import com.thock.back.settlement.settlement.out.MonthlySettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;


// 판매자가 확인하는 본인의 정산 내역 및 세부 정산서 관련 서비스

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementQueryService {

    private final DailySettlementRepository dailySettlementRepository;
    private final MonthlySettlementRepository monthlySettlementRepository;

    public List<MonthlySettlementView> getMonthlySummary(Long sellerId, YearMonth targetMonth) {
        String targetYearMonth = targetMonth.toString().replace("-", "");
        List<MonthlySettlement> rows = monthlySettlementRepository.findBySellerIdAndTargetYearMonth(sellerId, targetYearMonth);
        return rows.stream()
                .sorted(Comparator.comparing(MonthlySettlement::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(row -> new MonthlySettlementView(
                        row.getId(),
                        row.getSellerId(),
                        row.getTargetYearMonth(),
                        row.getTotalCount(),
                        row.getTotalPaymentAmount() == null ? null : row.getTotalPaymentAmount().amount(),
                        row.getTotalFeeAmount() == null ? null : row.getTotalFeeAmount().amount(),
                        row.getTotalPayoutAmount() == null ? null : row.getTotalPayoutAmount().amount(),
                        row.getStatus().name(),
                        row.getCreatedAt(),
                        row.getCompletedAt()
                ))
                .toList();
    }

    public List<DailySettlementItemView> getDailyItems(Long sellerId, LocalDate targetDate) {
        List<DailySettlement> settlements = dailySettlementRepository.findBySellerIdAndTargetDate(sellerId, targetDate);
        return settlements.stream()
                .flatMap(settlement -> settlement.getItems().stream()
                        .map(item -> toView(settlement, item)))
                .sorted(Comparator.comparing(DailySettlementItemView::dailySettlementId))
                .toList();
    }

    private DailySettlementItemView toView(DailySettlement settlement, DailySettlementItem item) {
        return new DailySettlementItemView(
                settlement.getId(),
                settlement.getSellerId(),
                settlement.getTargetDate(),
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getFinalQuantity(),
                item.getFinalAmount().amount(),
                settlement.getStatus().name()
        );
    }
}
