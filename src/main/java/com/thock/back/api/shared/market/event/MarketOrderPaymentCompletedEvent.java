package com.thock.back.api.shared.market.event;

import com.thock.back.api.shared.market.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketOrderPaymentCompletedEvent {
    private OrderDto order;
}
