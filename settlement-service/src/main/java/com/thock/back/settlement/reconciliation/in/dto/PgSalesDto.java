package com.thock.back.settlement.reconciliation.in.dto;

import com.thock.back.settlement.reconciliation.domain.PgSalesRaw;
import com.thock.back.settlement.reconciliation.domain.enums.PaymentMethod;
import com.thock.back.settlement.reconciliation.domain.enums.PgStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PgSalesDto (
        String pgKey,
        String merchantUid,
        PaymentMethod paymentMethod,
        BigDecimal paymentAmount,
        PgStatus pgStatus,
        LocalDateTime transactedAt
){
    public PgSalesRaw toEntity(){
        return PgSalesRaw.builder()
                .pgKey(this.pgKey)
                .merchantUid(this.merchantUid)
                .paymentMethod(this.paymentMethod)
                .paymentAmount(this.paymentAmount)
                .pgStatus(this.pgStatus)
                .transactedAt(this.transactedAt)
                .build();
    }
}
