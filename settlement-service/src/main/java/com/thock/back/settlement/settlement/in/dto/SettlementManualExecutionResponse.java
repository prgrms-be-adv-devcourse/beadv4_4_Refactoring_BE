package com.thock.back.settlement.settlement.in.dto;

import java.time.LocalDate;
import java.time.YearMonth;

public record SettlementManualExecutionResponse(
        String executionType,
        String target,
        Long batchId,
        Long executionId,
        String status
) {
    public static SettlementManualExecutionResponse daily(LocalDate targetDate) {
        return new SettlementManualExecutionResponse(
                "daily-settlement",
                targetDate.toString(),
                null,
                null,
                "done"
        );
    }

    public static SettlementManualExecutionResponse monthly(YearMonth targetMonth) {
        return new SettlementManualExecutionResponse(
                "monthly-settlement",
                targetMonth.toString(),
                null,
                null,
                "done"
        );
    }

    public static SettlementManualExecutionResponse dailyBatch(LocalDate targetDate, Long batchId, Long executionId, String status) {
        return new SettlementManualExecutionResponse(
                "daily-settlement-batch",
                targetDate.toString(),
                batchId,
                executionId,
                status
        );
    }

    public static SettlementManualExecutionResponse monthlyBatch(YearMonth targetMonth, Long batchId, Long executionId, String status) {
        return new SettlementManualExecutionResponse(
                "monthly-settlement-batch",
                targetMonth.toString(),
                batchId,
                executionId,
                status
        );
    }
}
