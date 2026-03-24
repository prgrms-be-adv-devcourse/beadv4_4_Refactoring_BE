package com.thock.back.settlement.settlement.app.useCase;

import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.settlement.settlement.domain.DailySettlement;
import com.thock.back.settlement.settlement.out.DailySettlementRepository;
import com.thock.back.settlement.settlement.out.MonthlySettlementRepository;
import com.thock.back.settlement.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RunMonthlySettlementUseCasePerfTest {

    @Autowired
    private RunMonthlySettlementUseCase runMonthlySettlementUseCase;

    @Autowired
    private DailySettlementRepository dailySettlementRepository;

    @Autowired
    private MonthlySettlementRepository monthlySettlementRepository;

    @MockBean
    private EventPublisher eventPublisher;

    @Test
    @DisplayName("월정산 베이스라인 성능 측정")
    void benchmarkMonthlySettlementBaseline() {
        // Baseline run 전에 테스트 데이터/결과를 정리한다.
        monthlySettlementRepository.deleteAllInBatch();
        dailySettlementRepository.deleteAllInBatch();

        YearMonth targetMonth = YearMonth.of(2026, 1);
        int sellerCount = 300;
        int days = targetMonth.lengthOfMonth();

        List<DailySettlement> seed = new ArrayList<>(sellerCount * days);
        for (long sellerId = 1; sellerId <= sellerCount; sellerId++) {
            for (int day = 1; day <= days; day++) {
                LocalDate targetDate = targetMonth.atDay(day);
                DailySettlement daily = DailySettlement.create(sellerId, targetDate);
                Money payment = Money.of(10_000L);
                Money fee = Money.of(2_000L);
                daily.updateAmounts(payment, fee, payment.minus(fee));
                seed.add(daily);
            }
        }
        dailySettlementRepository.saveAll(seed);

        long startedAt = System.nanoTime();
        runMonthlySettlementUseCase.execute(targetMonth);
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;

        long monthlyRows = monthlySettlementRepository.count();
        System.out.printf(
                "[PERF] monthly baseline: sellers=%d, dailyRows=%d, monthlyRows=%d, elapsedMs=%d%n",
                sellerCount,
                seed.size(),
                monthlyRows,
                elapsedMs
        );

        assertThat(monthlyRows).isEqualTo(sellerCount);
    }
}
