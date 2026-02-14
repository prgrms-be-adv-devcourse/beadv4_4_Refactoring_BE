package com.thock.back.settlement.settlement.app.service;

import com.thock.back.settlement.shared.money.Money;
import com.thock.back.settlement.settlement.domain.SettlementFeePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// 고정 수수료 정책 및 수수료 계산 객체화
@Component
public class FixedRateSettlementFeePolicy implements SettlementFeePolicy {

    private final BigDecimal feeRate;

    public FixedRateSettlementFeePolicy(@Value("${settlement.fee.rate:0.2}") BigDecimal feeRate) {
        if (feeRate.compareTo(BigDecimal.ZERO) < 0 || feeRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("settlement.fee.rate must be between 0 and 1");
        }
        this.feeRate = feeRate;
    }

    @Override
    public Money calculateFee(Money paymentAmount) {
        return paymentAmount.multiply(feeRate);
    }
}
