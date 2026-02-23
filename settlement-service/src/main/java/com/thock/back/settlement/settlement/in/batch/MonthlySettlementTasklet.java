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

import java.time.YearMonth;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class MonthlySettlementTasklet implements Tasklet {

    private final SettlementFacade settlementFacade;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String targetMonthParam = contribution.getStepExecution().getJobParameters().getString("targetMonth");
        YearMonth targetMonth = targetMonthParam == null ? YearMonth.now() : YearMonth.parse(targetMonthParam);

        log.info("[Batch] 월별 정산 Tasklet 시작 - targetMonth={}", targetMonth);
        settlementFacade.runMonthlySettlement(targetMonth);
        log.info("[Batch] 월별 정산 Tasklet 종료 - targetMonth={}", targetMonth);

        return RepeatStatus.FINISHED;
    }
}
