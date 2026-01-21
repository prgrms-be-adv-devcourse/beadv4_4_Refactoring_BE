package com.thock.back.api.shared.market.event;

import com.thock.back.api.shared.market.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketOrderPaymentRequestedEvent {
    private OrderDto order;
    private Long pgAmount;
    private String paymentKey;
}
