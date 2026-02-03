package com.thock.back.settlement.reconciliation.in.dto;

import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.domain.enums.OrderEventStatus;
import com.thock.back.settlement.reconciliation.domain.enums.SettlementStatus;
import com.thock.back.settlement.shared.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record OrderItemMessageDto(
        String orderNo,
        Long sellerId,
        String productName,
        int productQuantity,
        BigDecimal productAmount,
        BigDecimal paymentAmount, // 양수로 받고, 서비스에서 음수처리하는게 나을듯
        String eventType, // 결제 완료인지, 구매확정인지, 환불인지 알려주는 필드, 이걸 기준으로 결제/환불 매핑
        Map<String, Object> metadata,
        LocalDateTime snapshotAt
) {
    public SalesLog toEntity() {

        // String으로 받은 것을, 일단 Enum으로 매칭(결제 완료, 구매확정, 환불)
        OrderEventStatus eventStatus = OrderEventStatus.from(eventType);

        // 위에서 바뀐 Enum을 토대로 DB에 저장해야하는 TransactionType으로 매핑
        TransactionType transactionType = eventStatus.getTransactionType();

        // 환불일 경우 음수로 저장
        BigDecimal finalAmount = this.paymentAmount;
        if(transactionType == TransactionType.REFUND){
            finalAmount = finalAmount.abs().negate();
        }

        return SalesLog.builder()
                .orderNo(this.orderNo)
                .sellerId(this.sellerId)
                .productName(this.productName)
                .productQuantity(this.productQuantity)
                .productAmount(this.productAmount)
                .paymentAmount(finalAmount)
                .transactionType(transactionType)
                .metadata(this.metadata)
                .snapshotAt(this.snapshotAt)
                .settlementStatus(SettlementStatus.WAIT)
                .build();
    }
}
