package com.thock.back.settlement.settlement.in.batch;

import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.shared.settlement.event.SettlementCompletedEvent;
import com.thock.back.settlement.settlement.domain.MonthlySettlement;
import com.thock.back.settlement.settlement.domain.enums.MonthlySettlementStatus;
import com.thock.back.settlement.settlement.out.MonthlySettlementRepository;
import com.thock.back.settlement.shared.money.Money;
import com.thock.back.settlement.shared.slack.event.MonthlySettlementFailedAlertEvent;
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

    private static final int MAX_RETRY_PER_RUN = 3;

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
            MonthlySettlement monthlySettlement = monthlySettlementRepository
                    .findFirstBySellerIdAndTargetYearMonth(item.sellerId(), targetYearMonth)
                    .orElseGet(() -> monthlySettlementRepository.save(
                            MonthlySettlement.builder()
                                    .sellerId(item.sellerId())
                                    .targetYearMonth(targetYearMonth)
                                    .totalCount(item.totalCount())
                                    .totalPaymentAmount(Money.of(item.totalPaymentAmount()))
                                    .totalFeeAmount(Money.of(item.totalFeeAmount()))
                                    .totalPayoutAmount(Money.of(item.totalPayoutAmount()))
                                    .build()
                    ));

            if (monthlySettlement.getStatus() == MonthlySettlementStatus.COMPLETED) {
                log.info("이미 완료된 월별 정산이라 스킵합니다. sellerId={}, targetYearMonth={}", item.sellerId(), targetYearMonth);
                continue;
            }

            if (monthlySettlement.getStatus() == MonthlySettlementStatus.FAILED) {
                log.info("재시도 한도 초과(FAILED) 상태라 스킵합니다. sellerId={}, targetYearMonth={}", item.sellerId(), targetYearMonth);
                continue;
            }

            monthlySettlement.refreshTotals(
                    item.totalCount(),
                    Money.of(item.totalPaymentAmount()),
                    Money.of(item.totalFeeAmount()),
                    Money.of(item.totalPayoutAmount())
            );

            for (int attempt = 1; attempt <= MAX_RETRY_PER_RUN; attempt++) {
                try {
                    monthlySettlement.startPayout();

                    // 월별 정산 실패 시나리오 시 재처리 로직 및 슬랙 알림 테스트 코드
                    /* if (item.sellerId() % 10 == 0) {
                        throw new IllegalStateException("forced failure for retry test");
                    } */

                    eventPublisher.publish(new SettlementCompletedEvent(item.sellerId(), item.totalPayoutAmount()));
                    monthlySettlement.completePayout();
                    break;
                } catch (Exception e) {
                    monthlySettlement.failPayout(e.getMessage());
                    log.warn("월별 정산 처리 실패. sellerId={}, targetYearMonth={}, retryCount={}, attempt={}",
                            item.sellerId(), targetYearMonth, monthlySettlement.getRetryCount(), attempt, e);
                    if (monthlySettlement.getStatus() == MonthlySettlementStatus.FAILED) {
                        eventPublisher.publish(new MonthlySettlementFailedAlertEvent(
                                monthlySettlement.getSellerId(),
                                monthlySettlement.getTargetYearMonth(),
                                monthlySettlement.getRetryCount(),
                                e.getClass().getSimpleName() + ": " + e.getMessage()
                        ));
                        break;
                    }
                }
            }
        }
    }
}
