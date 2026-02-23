package com.thock.back.settlement.reconciliation.in.mapper;

import com.thock.back.settlement.reconciliation.in.dto.OrderItemMessageDto;
import com.thock.back.shared.settlement.dto.SettlementOrderItemDto;
import org.springframework.stereotype.Component;

@Component
public class SettlementOrderItemMapper {

    // common에서 받은 카프카 메세지 dto를 내부 OrderItemMessage Dto로 변환
    public OrderItemMessageDto toOrderItemMessageDto(SettlementOrderItemDto item) {
        return new OrderItemMessageDto(
                item.orderNo(),
                item.sellerId(),
                item.productId(),
                item.productName(),
                item.productQuantity(),
                item.productPrice(),
                item.paymentAmount(),
                item.eventType(),
                item.metadata(),
                item.snapshotAt()
        );
    }
}
