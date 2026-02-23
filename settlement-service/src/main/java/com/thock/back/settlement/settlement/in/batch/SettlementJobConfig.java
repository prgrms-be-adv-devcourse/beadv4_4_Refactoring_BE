package com.thock.back.settlement.settlement.in.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SettlementJobConfig {

    @Bean
    public Job dailySettlementJob(
            JobRepository jobRepository,
            Step dailySettlementStep
    ) {
        return new JobBuilder("dailySettlementJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(dailySettlementStep)
                .build();
    }

    @Bean
    public Step dailySettlementStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DailySettlementTasklet dailySettlementTasklet
    ) {
        return new StepBuilder("dailySettlementStep", jobRepository)
                .tasklet(dailySettlementTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job monthlySettlementJob(
            JobRepository jobRepository,
            Step monthlySettlementStep
    ) {
        return new JobBuilder("monthlySettlementJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(monthlySettlementStep)
                .build();
    }

    @Bean
    public Step monthlySettlementStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            MonthlySettlementTasklet monthlySettlementTasklet
    ) {
        return new StepBuilder("monthlySettlementStep", jobRepository)
                .tasklet(monthlySettlementTasklet, transactionManager)
                .build();
    }
}
