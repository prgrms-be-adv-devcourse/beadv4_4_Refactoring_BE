package com.thock.back.settlement.settlement.in.batch;

import com.thock.back.settlement.settlement.app.SettlementFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class DailySettlementTasklet implements Tasklet {

    private final SettlementFacade settlementFacade;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String targetDateParam = contribution.getStepExecution().getJobParameters().getString("targetDate");
        LocalDate targetDate = targetDateParam == null ? LocalDate.now() : LocalDate.parse(targetDateParam);

        log.info("[Batch] 일별 정산 Tasklet 시작 - targetDate={}", targetDate);
        settlementFacade.runDailySettlement(targetDate);
        log.info("[Batch] 일별 정산 Tasklet 종료 - targetDate={}", targetDate);

        return RepeatStatus.FINISHED;
    }
}
