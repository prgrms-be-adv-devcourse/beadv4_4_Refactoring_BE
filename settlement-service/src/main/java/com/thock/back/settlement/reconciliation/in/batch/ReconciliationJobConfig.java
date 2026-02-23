package com.thock.back.settlement.reconciliation.in.batch;

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
public class ReconciliationJobConfig {

    @Bean
    public Step reconciliationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ReconciliationTasklet reconciliationTasklet
    ){
        return new StepBuilder("reconciliationStep", jobRepository)
                .tasklet(reconciliationTasklet, transactionManager)
                .build();
    }

    @Bean
   public Job reconciliationJob(
           JobRepository jobRepository,
           Step reconciliationStep
   ) {
        return new JobBuilder("reconciliationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(reconciliationStep)
                .build();
   }

}
