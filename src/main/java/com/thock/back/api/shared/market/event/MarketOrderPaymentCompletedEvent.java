package com.thock.back.api.shared.market.event;

import com.thock.back.api.shared.market.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MarketOrderPaymentCompletedEvent {
    private final OrderDto order;
    private Long pgPaymentAmount;
}
