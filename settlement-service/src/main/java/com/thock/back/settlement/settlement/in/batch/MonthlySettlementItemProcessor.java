package com.thock.back.settlement.settlement.in.batch;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class MonthlySettlementItemProcessor implements ItemProcessor<MonthlySettlementAggDto, MonthlySettlementWriteModel> {

    @Override
    public MonthlySettlementWriteModel process(MonthlySettlementAggDto item) {
        return new MonthlySettlementWriteModel(
                item.sellerId(),
                item.totalCount(),
                item.totalPaymentAmount(),
                item.totalFeeAmount(),
                item.totalPayoutAmount()
        );
    }
}
