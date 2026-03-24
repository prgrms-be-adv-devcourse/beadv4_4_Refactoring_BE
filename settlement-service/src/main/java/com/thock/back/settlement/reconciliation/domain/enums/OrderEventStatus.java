package com.thock.back.settlement.reconciliation.domain.enums;

import com.thock.back.settlement.shared.enums.TransactionType;
import lombok.Getter;

@Getter
public enum OrderEventStatus {
    PAYMENT_COMPLETED(TransactionType.PAYMENT),
    PURCHASE_CONFIRMED(TransactionType.PAYMENT),
    REFUND_COMPLETED(TransactionType.REFUND),
    UNKNOWN(null);

    private final TransactionType transactionType;

    OrderEventStatus(TransactionType transactionType){
        this.transactionType = transactionType;
    }

    public static OrderEventStatus from(String status) {
        try{
            return valueOf(status);
        } catch (Exception e){
            return UNKNOWN;
        }
    }
}
// enum 값 매핑해주는것과 OrderItemMessageDto에서 status 어떻게 할건지, transactionType이 없는데 어떻게 매핑할건지