package com.thock.back.settlement.settlement.domain;

import com.thock.back.settlement.shared.money.Money;

public interface SettlementFeePolicy {
    Money calculateFee(Money paymentAmount);
}
