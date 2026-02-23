package com.thock.back.settlement.reconciliation.app.useCase;

import com.thock.back.settlement.reconciliation.domain.PgSalesRaw;
import com.thock.back.settlement.reconciliation.domain.ReconciliationResult;
import com.thock.back.settlement.reconciliation.domain.ReconciliationMismatchLog;
import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.domain.enums.MismatchType;
import com.thock.back.settlement.reconciliation.domain.enums.PgStatus;
import com.thock.back.settlement.reconciliation.domain.enums.ReconciliationStatus;
import com.thock.back.settlement.reconciliation.out.ReconciliationJobRepository;
import com.thock.back.settlement.reconciliation.out.ReconciliationMismatchLogRepository;
import com.thock.back.settlement.reconciliation.out.PgSalesRawRepository;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import com.thock.back.settlement.shared.enums.TransactionType;
import com.thock.back.settlement.shared.money.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunReconciliationUseCase {
    private final SalesLogRepository salesLogRepository;
    private final PgSalesRawRepository pgSalesRawRepository;
    private final ReconciliationJobRepository jobRepository;
    private final ReconciliationMismatchLogRepository mismatchLogRepository;


    @Transactional
    public void execute(LocalDate date) {
        log.info("=========[대사 시작] 기준일: {} =========", date);

        // 대사 결과를 담는 job 생성
        ReconciliationResult job = ReconciliationResult.builder().
                baseDate(date).build();
        jobRepository.save(job);

        // PG 데이터 조회
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<PgSalesRaw> pgList = pgSalesRawRepository.findAllByTransactedAtBetween(startOfDay, endOfDay);

        // 부분 환불, 부분 확정을 도입하면서 PG사의 데이터를 일대일로 비교할 수 없게됐음.
        // 이에 따라 주문번호 + 가격으로 그루핑 해서 map에 저장 후, 그 값을 DB 값과 비교.
        // TODO: MVP 단계에서는 대용량을 생각하지 말고, 기능구현에 집중. 이후 Batch 도입 고려
        // TODO: 대사의 과정이 절차적으로 표현되어 있어 가독성 떨어지니, 메소드 단위로 구분해 클린코드 만들기

        Map<String, Long> pgSumMap = pgList.stream()
                .collect(Collectors.groupingBy(
                        pg -> pg.getMerchantUid() + "_" + pg.getPgStatus().name(),
                        Collectors.summingLong(pg -> pg.getPaymentAmount().amount())
                ));

        int successCount = 0;
        int mismatchCount = 0;

        // 중복 검사 체크용 리스트
        List<String> processedKeys = pgSumMap.keySet().stream().toList();

        // 여기서 key = 주문번호_PG상태
        for (String key : processedKeys) {
            String[] split = key.split("_");
            String merchantUid = split[0];
            PgStatus pgStatus = PgStatus.valueOf(split[1]);
            Long pgTotalAmount = pgSumMap.get(key);

            //
            TransactionType targetType = convertToTransactionType(pgStatus);
            if (targetType == null) continue;

            List<SalesLog> internalLogs = salesLogRepository.findByOrderNoAndTransactionType(
                    merchantUid,
                    targetType
            );

            if (internalLogs.isEmpty()) {
                // 대표로 사용할 PG Raw 데이터 하나 찾기 (로그용)
                PgSalesRaw samplePg = pgList.stream()
                        .filter(p -> p.getMerchantUid().equals(merchantUid) && p.getPgStatus() == pgStatus)
                        .findFirst().orElse(null);

                List<SalesLog> sameOrderLogs = salesLogRepository.findByOrderNo(merchantUid);
                if (!sameOrderLogs.isEmpty()) {
                    sameOrderLogs.forEach(SalesLog::mismatchReconciliation);
                    long internalTotal = sameOrderLogs.stream()
                            .mapToLong(log -> log.getPaymentAmount().amount())
                            .sum();
                    saveMismatchLog(
                            job,
                            samplePg,
                            sameOrderLogs.get(0),
                            Money.of(internalTotal),
                            MismatchType.STATUS_DIFF,
                            "상태 불일치"
                    );
                } else {
                    saveMismatchLog(job, samplePg, null, Money.zero(), MismatchType.PG_ONLY, "주문서 누락");
                }
                mismatchCount++;
                continue;
            }

            long dbSum = internalLogs.stream().mapToLong(log -> log.getPaymentAmount().amount()).sum();

            if (pgTotalAmount == Math.abs(dbSum)) {
                internalLogs.forEach(SalesLog::matchReconciliation);
                successCount++;
            } else {
                internalLogs.forEach(SalesLog::mismatchReconciliation);
                PgSalesRaw samplePg = pgList.stream()
                        .filter(p -> p.getMerchantUid().equals(merchantUid) && p.getPgStatus() == pgStatus)
                        .findFirst().orElse(null);

                // [수정 2 핵심] 첫 번째 항목의 금액이 아닌, 합산된 금액(dbSum)을 파라미터로 넘김
                saveMismatchLog(job, samplePg, internalLogs.get(0), Money.of(dbSum), MismatchType.AMOUNT_DIFF, "금액 불일치");
                mismatchCount++;
            }
        }

        List<SalesLog> remainLogs = salesLogRepository.findAllBySnapshotAtBetweenAndReconciliationStatus(
                startOfDay,
                endOfDay,
                ReconciliationStatus.PENDING
        );

        for (SalesLog remainLog : remainLogs) {
            remainLog.mismatchReconciliation();
            saveMismatchLog(job, null, remainLog, remainLog.getPaymentAmount(), MismatchType.INTERNAL_ONLY, "PG 내역 없음");
            mismatchCount++;
        }

        job.finish(processedKeys.size(), successCount, mismatchCount);
        log.info("=========[대사 종료] 성공: {}, 실패: {}", successCount, mismatchCount);
    }

    private TransactionType convertToTransactionType(PgStatus pgStatus) {
        if (pgStatus == PgStatus.PAID) {
            return TransactionType.PAYMENT;
        }
        if (pgStatus == PgStatus.CANCELED) {
            return TransactionType.REFUND;
        }
        return null;
    }

    private void saveMismatchLog(ReconciliationResult job, PgSalesRaw pg, SalesLog internal,
                                 Money calculatedInternalAmount, MismatchType type, String reason) {

        String orderNo = (pg != null) ? pg.getMerchantUid() : internal.getOrderNo();
        String pgKey = (pg != null) ? pg.getPgKey() : null;
        Money pgAmount = (pg != null && pg.getPaymentAmount() != null) ? pg.getPaymentAmount() : Money.zero();

        ReconciliationMismatchLog log = ReconciliationMismatchLog.builder()
                .job(job)
                .orderNo(orderNo)
                .pgKey(pgKey)
                .type(type)
                .pgAmount(pgAmount)
                .internalAmount(calculatedInternalAmount) // 합산된 금액으로 저장
                .reason(reason)
                .build();
        mismatchLogRepository.save(log);
    }
}
