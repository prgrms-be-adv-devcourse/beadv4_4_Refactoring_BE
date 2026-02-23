package com.thock.back.settlement.settlement.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class SettlementBatchLauncher {

    private final JobLauncher jobLauncher;

    @Qualifier("dailySettlementJob")
    private final Job dailySettlementJob;

    @Qualifier("monthlySettlementJob")
    private final Job monthlySettlementJob;

    public BatchRunResult runDaily(LocalDate targetDate) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDate", targetDate.toString())
                    .addLong("requestedAt", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(dailySettlementJob, jobParameters);
            return new BatchRunResult(execution.getJobId(), execution.getId(), execution.getStatus().name());
        } catch (Exception e) {
            throw new IllegalStateException("일별 정산 배치 실행에 실패했습니다.", e);
        }
    }

    public BatchRunResult runMonthly(YearMonth targetMonth) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetMonth", targetMonth.toString())
                    .addLong("requestedAt", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(monthlySettlementJob, jobParameters);
            return new BatchRunResult(execution.getJobId(), execution.getId(), execution.getStatus().name());
        } catch (Exception e) {
            throw new IllegalStateException("월별 정산 배치 실행에 실패했습니다.", e);
        }
    }

    public record BatchRunResult(Long batchId, Long executionId, String status) {
    }
}
