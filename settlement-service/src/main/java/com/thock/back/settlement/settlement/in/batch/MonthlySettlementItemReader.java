package com.thock.back.settlement.settlement.in.batch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Iterator;
import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
public class MonthlySettlementItemReader implements ItemReader<MonthlySettlementAggDto> {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("#{jobParameters['targetMonth']}")
    private String targetMonthParam;

    private Iterator<MonthlySettlementAggDto> iterator;

    @Override
    public MonthlySettlementAggDto read() {
        if (iterator == null) {
            YearMonth targetMonth = targetMonthParam == null ? YearMonth.now() : YearMonth.parse(targetMonthParam);
            LocalDate start = targetMonth.atDay(1);
            LocalDate end = targetMonth.atEndOfMonth();

            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.createNativeQuery("""
                    SELECT seller_id,
                           COUNT(*) AS total_count,
                           SUM(payment_amount) AS total_payment_amount,
                           SUM(fee_amount) AS total_fee_amount,
                           SUM(settlement_amount) AS total_payout_amount
                    FROM daily_settlement
                    WHERE target_date BETWEEN :start AND :end
                    GROUP BY seller_id
                    """)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getResultList();

            List<MonthlySettlementAggDto> aggregates = rows.stream()
                    .map(row -> new MonthlySettlementAggDto(
                            toLong(row[0]),
                            toLong(row[1]),
                            toLong(row[2]),
                            toLong(row[3]),
                            toLong(row[4])
                    ))
                    .toList();
            iterator = aggregates.iterator();
        }

        return iterator.hasNext() ? iterator.next() : null;
    }

    private Long toLong(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.longValue();
        }
        return ((Number) value).longValue();
    }
}
