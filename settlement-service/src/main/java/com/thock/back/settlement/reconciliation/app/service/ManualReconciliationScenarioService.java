package com.thock.back.settlement.reconciliation.app.service;

import com.thock.back.settlement.reconciliation.app.ReconciliationFacade;
import com.thock.back.settlement.reconciliation.domain.PgSalesRaw;
import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.domain.enums.PgStatus;
import com.thock.back.settlement.reconciliation.domain.enums.PaymentMethod;
import com.thock.back.settlement.reconciliation.out.PgSalesRawRepository;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import com.thock.back.settlement.settlement.app.SettlementFacade;
import com.thock.back.settlement.shared.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManualReconciliationScenarioService {

    private final SalesLogRepository salesLogRepository;
    private final PgSalesRawRepository pgSalesRawRepository;
    private final ReconciliationFacade reconciliationFacade;
    private final SettlementFacade settlementFacade;

    @Transactional
    public GeneratePgResult generatePgRawFromSalesLogs(LocalDate targetDate) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        List<SalesLog> logs = salesLogRepository.findAllBySnapshotAtBetween(start, end);
        if (logs.isEmpty()) {
            return new GeneratePgResult(targetDate, 0, 0, 0);
        }

        List<SalesLog> targetLogs = logs.stream()
                .filter(log -> log.getTransactionType() == TransactionType.PAYMENT
                        || log.getTransactionType() == TransactionType.REFUND)
                .toList();

        Map<String, List<SalesLog>> grouped = targetLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getOrderNo() + "|" + log.getTransactionType().name()));

        // targetDate의 기존 PG RAW는 제거하고 다시 생성 (수동 재실행 시 중복 방지)
        List<PgSalesRaw> existing = pgSalesRawRepository.findAllByTransactedAtBetween(start, end);
        if (!existing.isEmpty()) {
            pgSalesRawRepository.deleteAll(existing);
        }

        List<PgSalesRaw> generated = grouped.entrySet().stream()
                .map(entry -> toPgRaw(entry.getValue(), targetDate))
                .filter(Objects::nonNull)
                .toList();

        if (!generated.isEmpty()) {
            pgSalesRawRepository.saveAll(generated);
        }

        return new GeneratePgResult(
                targetDate,
                logs.size(),
                generated.size(),
                generated.stream().mapToLong(PgSalesRaw::getPaymentAmount).sum()
        );
    }

    public void runReconciliation(LocalDate targetDate) {
        reconciliationFacade.runReconciliation(targetDate);
    }

    public void runDailySettlement(LocalDate targetDate) {
        settlementFacade.runDaily(targetDate);
    }

    public void runMonthlySettlement(YearMonth targetMonth) {
        settlementFacade.runMonthly(targetMonth);
    }

    @Transactional
    public RunAllResult runAll(LocalDate targetDate) {
        GeneratePgResult generate = generatePgRawFromSalesLogs(targetDate);
        runReconciliation(targetDate);
        runDailySettlement(targetDate);
        runMonthlySettlement(YearMonth.from(targetDate));
        return new RunAllResult(targetDate, YearMonth.from(targetDate), generate.generatedPgRows());
    }

    private PgSalesRaw toPgRaw(List<SalesLog> group, LocalDate targetDate) {
        if (group.isEmpty()) {
            return null;
        }

        SalesLog sample = group.get(0);
        TransactionType txType = sample.getTransactionType();
        PgStatus pgStatus = txType == TransactionType.REFUND ? PgStatus.CANCELED : PgStatus.PAID;
        long amount = Math.abs(group.stream().mapToLong(SalesLog::getPaymentAmount).sum());

        return PgSalesRaw.builder()
                .pgKey("MANUAL-" + sample.getOrderNo() + "-" + pgStatus + "-" + System.currentTimeMillis())
                .merchantUid(sample.getOrderNo())
                .paymentMethod(sample.getPaymentMethod() == null ? PaymentMethod.CARD : sample.getPaymentMethod())
                .paymentAmount(amount)
                .pgStatus(pgStatus)
                .transactedAt(LocalDateTime.of(targetDate, LocalTime.of(12, 0)))
                .build();
    }

    public record GeneratePgResult(
            LocalDate targetDate,
            int sourceSalesLogs,
            int generatedPgRows,
            long totalGeneratedAmount
    ) {}

    public record RunAllResult(
            LocalDate targetDate,
            YearMonth targetMonth,
            int generatedPgRows
    ) {}
}
