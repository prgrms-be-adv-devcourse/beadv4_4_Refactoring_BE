package com.thock.back.shared.market.event;

import com.thock.back.shared.market.dto.MarketMemberDto;

public record MarketMemberCreatedEvent (
        MarketMemberDto member
) { }
