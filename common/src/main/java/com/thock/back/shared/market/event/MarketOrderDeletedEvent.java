package com.thock.back.shared.market.event;

import com.thock.back.shared.market.dto.OrderDeleteRequestDto;

public record MarketOrderDeletedEvent (
        OrderDeleteRequestDto dto
){
}
