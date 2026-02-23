package com.thock.back.settlement.settlement.in.batch;

import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.shared.settlement.event.SettlementCompletedEvent;
import com.thock.back.settlement.settlement.domain.MonthlySettlement;
import com.thock.back.settlement.settlement.out.MonthlySettlementRepository;
import com.thock.back.settlement.shared.money.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class MonthlySettlementItemWriter implements ItemWriter<MonthlySettlementWriteModel> {

    private final MonthlySettlementRepository monthlySettlementRepository;
    private final EventPublisher eventPublisher;

    @Value("#{jobParameters['targetMonth']}")
    private String targetMonthParam;

    @Override
    @Transactional
    public void write(Chunk<? extends MonthlySettlementWriteModel> chunk) {
        YearMonth targetMonth = targetMonthParam == null ? YearMonth.now() : YearMonth.parse(targetMonthParam);
        String targetYearMonth = targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));

        for (MonthlySettlementWriteModel item : chunk) {
            if (monthlySettlementRepository.existsBySellerIdAndTargetYearMonth(item.sellerId(), targetYearMonth)) {
                log.info("이미 생성된 월별 정산이 있어 스킵합니다. sellerId={}, targetYearMonth={}", item.sellerId(), targetYearMonth);
                continue;
            }

            MonthlySettlement monthlySettlement = MonthlySettlement.builder()
                    .sellerId(item.sellerId())
                    .targetYearMonth(targetYearMonth)
                    .totalCount(item.totalCount())
                    .totalPaymentAmount(Money.of(item.totalPaymentAmount()))
                    .totalFeeAmount(Money.of(item.totalFeeAmount()))
                    .totalPayoutAmount(Money.of(item.totalPayoutAmount()))
                    .build();

            monthlySettlementRepository.save(monthlySettlement);
            eventPublisher.publish(new SettlementCompletedEvent(item.sellerId(), item.totalPayoutAmount()));
        }
    }
}
