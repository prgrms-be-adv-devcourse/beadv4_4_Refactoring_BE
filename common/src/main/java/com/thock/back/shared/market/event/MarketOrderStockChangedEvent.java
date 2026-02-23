package com.thock.back.shared.market.event;

import com.thock.back.shared.market.domain.StockEventType;
import com.thock.back.shared.market.dto.StockOrderItemDto;

import java.util.List;

public record MarketOrderStockChangedEvent(
        String orderNumber,
        StockEventType eventType,
        List<StockOrderItemDto> items
) {}
