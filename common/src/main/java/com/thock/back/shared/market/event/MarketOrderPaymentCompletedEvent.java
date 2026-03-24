package com.thock.back.shared.market.event;

import com.thock.back.shared.market.dto.OrderDto;

public record MarketOrderPaymentCompletedEvent(
        OrderDto order
) {}
