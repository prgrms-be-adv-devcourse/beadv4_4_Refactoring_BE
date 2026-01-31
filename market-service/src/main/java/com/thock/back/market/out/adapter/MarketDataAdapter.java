package com.thock.back.market.out.adapter;


import com.thock.back.market.domain.OrderItem;
import com.thock.back.market.domain.OrderItemState;
import com.thock.back.market.out.repository.OrderItemRepository;
import com.thock.back.shared.settlement.dto.SettlementOrderDto;
import com.thock.back.shared.settlement.port.MarketDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketDataAdapter implements MarketDataPort {

    private final OrderItemRepository orderItemRepository;
    @Override
    public List<SettlementOrderDto> getSettlementTargetOrders(Long sellerId, LocalDate date) {
        // 1. 구매 확정된 주문 아이템 조회
        List<OrderItem> items = orderItemRepository.findBySellerIdAndStatusAndDate(
                sellerId,
                OrderItemState.PAYMENT_COMPLETED,
                date
        );

        // 2. OrderItem -> SettlementOrderDto 변환
        return items.stream()
                .map(item -> SettlementOrderDto.builder()
                        .orderId(item.getOrder().getId())
                        .orderItemId(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .totalSalePrice(item.getTotalSalePrice())
                        .payoutAmount(item.getPayoutAmount())
                        .feeAmount(item.getFeeAmount())
                        .confirmedAt(item.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
