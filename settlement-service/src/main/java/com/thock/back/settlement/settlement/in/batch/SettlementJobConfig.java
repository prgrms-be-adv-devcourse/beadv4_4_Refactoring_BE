package com.thock.back.settlement.settlement.in.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SettlementJobConfig {

    private static final int MONTHLY_SETTLEMENT_CHUNK_SIZE = 200;

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
            ItemReader<MonthlySettlementAggDto> monthlySettlementItemReader,
            ItemProcessor<MonthlySettlementAggDto, MonthlySettlementWriteModel> monthlySettlementItemProcessor,
            ItemWriter<MonthlySettlementWriteModel> monthlySettlementItemWriter
    ) {
        return new StepBuilder("monthlySettlementStep", jobRepository)
                .<MonthlySettlementAggDto, MonthlySettlementWriteModel>chunk(MONTHLY_SETTLEMENT_CHUNK_SIZE, transactionManager)
                .reader(monthlySettlementItemReader)
                .processor(monthlySettlementItemProcessor)
                .writer(monthlySettlementItemWriter)
                .build();
    }
}
