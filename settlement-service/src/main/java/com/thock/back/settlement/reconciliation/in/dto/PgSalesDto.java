package com.thock.back.settlement.reconciliation.in.dto;

import com.thock.back.settlement.reconciliation.domain.PgSalesRaw;
import com.thock.back.settlement.reconciliation.domain.enums.PaymentMethod;
import com.thock.back.settlement.reconciliation.domain.enums.PgStatus;
import com.thock.back.settlement.shared.money.Money;

import java.time.LocalDateTime;

public record PgSalesDto (
        String pgKey,
        String merchantUid,
        PaymentMethod paymentMethod,
        Long paymentAmount,
        PgStatus pgStatus,
        LocalDateTime transactedAt
){
    public PgSalesRaw toEntity(){
        return PgSalesRaw.builder()
                .pgKey(this.pgKey)
                .merchantUid(this.merchantUid)
                .paymentMethod(this.paymentMethod)
                .paymentAmount(Money.of(this.paymentAmount))
                .pgStatus(this.pgStatus)
                .transactedAt(this.transactedAt)
                .build();
    }
}
