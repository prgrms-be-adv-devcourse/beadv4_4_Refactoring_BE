package com.thock.back.api.boundedContext.market.app;

import com.thock.back.api.boundedContext.market.domain.Cart;
import com.thock.back.api.boundedContext.market.domain.MarketMember;
import com.thock.back.api.shared.market.dto.MarketMemberDto;
import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketFacade {

    private final MarketSyncMemberUseCase marketSyncMemberUseCase;
    private final MarketCreateCartUseCase marketCreateCartUseCase;
    @Transactional
    public MarketMember syncMember(MemberDto member) {
        return marketSyncMemberUseCase.syncMember(member);
    }

    @Transactional
    public Cart createCart(MarketMemberDto buyer) {
        return marketCreateCartUseCase.createCart(buyer);
    }
}
