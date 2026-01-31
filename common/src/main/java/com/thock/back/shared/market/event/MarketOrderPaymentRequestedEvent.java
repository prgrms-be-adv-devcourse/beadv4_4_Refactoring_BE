package com.thock.back.shared.market.event;

import com.thock.back.shared.market.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketOrderPaymentRequestedEvent {
    private OrderDto order;
    private Long pgAmount;
}
