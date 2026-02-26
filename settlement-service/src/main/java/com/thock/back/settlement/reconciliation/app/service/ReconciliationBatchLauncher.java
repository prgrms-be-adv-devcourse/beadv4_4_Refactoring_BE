package com.thock.back.settlement.reconciliation.app.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReconciliationBatchLauncher {

    private final JobLauncher jobLauncher;
    private final Job reconciliationJob;

    public ReconciliationBatchLauncher(
            JobLauncher jobLauncher,
            @Qualifier("reconciliationJob") Job reconciliationJob
    ) {
        this.jobLauncher = jobLauncher;
        this.reconciliationJob = reconciliationJob;
    }

    public BatchRunResult run(LocalDate targetDate) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDate", targetDate.toString())
                    .addLong("requestedAt", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(reconciliationJob, jobParameters);
            return new BatchRunResult(execution.getJobId(), execution.getId(), execution.getStatus().name());
        } catch (Exception e) {
            throw new IllegalStateException("대사 배치 실행에 실패했습니다.", e);
        }
    }

    public record BatchRunResult(Long batchId, Long executionId, String status) {
    }
}
