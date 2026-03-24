package com.thock.back.shared.market.event;

import com.thock.back.shared.market.dto.OrderDto;

public record MarketOrderPaymentRequestedEvent(
        OrderDto order,
        Long pgAmount
) {}
