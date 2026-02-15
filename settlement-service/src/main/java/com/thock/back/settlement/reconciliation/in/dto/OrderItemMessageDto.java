package com.thock.back.settlement.reconciliation.in.dto;

import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.domain.enums.OrderEventStatus;
import com.thock.back.settlement.reconciliation.domain.enums.ReconciliationStatus;
import com.thock.back.settlement.shared.enums.TransactionType;
import com.thock.back.settlement.shared.money.Money;

import java.time.LocalDateTime;
import java.util.Map;

public record OrderItemMessageDto(
        String orderNo,
        Long sellerId,
        Long productId,
        String productName,
        int productQuantity,
        Long productPrice,
        Long paymentAmount, // 양수로 받고, 서비스에서 음수처리하는게 나을듯
        String eventType, // 결제 완료인지, 구매확정인지, 환불인지 알려주는 필드, 이걸 기준으로 결제/환불 매핑
        Map<String, Object> metadata,
        LocalDateTime snapshotAt
) {
    public SalesLog toEntity() {

        // String으로 받은 것을, 일단 Enum으로 매칭(결제 완료, 구매확정, 환불)
        OrderEventStatus eventStatus = OrderEventStatus.from(eventType);

        // 위에서 바뀐 Enum을 토대로 DB에 저장해야하는 TransactionType으로 매핑
        TransactionType transactionType = eventStatus.getTransactionType();

        // 환불일 경우 수량과 가격을 음수로 저장
        long finalAmount = this.paymentAmount;
        int finalQuantity = this.productQuantity;

        if(transactionType == TransactionType.REFUND){
            finalAmount = Math.abs(this.paymentAmount) * -1L;
            finalQuantity = Math.abs(this.productQuantity) * -1;
        }

        return SalesLog.builder()
                .orderNo(this.orderNo)
                .sellerId(this.sellerId)
                .productId(this.productId)
                .productName(this.productName)
                .productQuantity(finalQuantity)
                .productPrice(Money.of(this.productPrice))
                .paymentAmount(Money.of(finalAmount))
                .transactionType(transactionType)
                .metadata(this.metadata)
                .snapshotAt(this.snapshotAt)
                .reconciliationStatus(ReconciliationStatus.PENDING)
                .build();
    }
}
