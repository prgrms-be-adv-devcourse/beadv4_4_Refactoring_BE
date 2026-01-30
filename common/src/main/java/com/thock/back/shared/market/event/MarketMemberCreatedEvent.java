package com.thock.back.shared.market.event;

import com.thock.back.shared.market.dto.MarketMemberDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketMemberCreatedEvent {
    private final MarketMemberDto member;
}
