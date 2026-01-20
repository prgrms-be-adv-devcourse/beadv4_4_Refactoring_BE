package com.thock.back.api.boundedContext.market.app;

import com.thock.back.api.boundedContext.market.domain.Cart;
import com.thock.back.api.boundedContext.market.domain.MarketMember;
import com.thock.back.api.boundedContext.market.in.dto.req.CartItemAddRequest;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemListResponse;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemResponse;
import com.thock.back.api.shared.market.dto.MarketMemberDto;
import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketFacade {

    private final MarketSyncMemberUseCase marketSyncMemberUseCase;
    private final MarketCreateCartUseCase marketCreateCartUseCase;
    private final CartService cartService;

    @Transactional
    public MarketMember syncMember(MemberDto member) {
        return marketSyncMemberUseCase.syncMember(member);
    }

    @Transactional
    public Cart createCart(MarketMemberDto buyer) {
        return marketCreateCartUseCase.createCart(buyer);
    }

    @Transactional(readOnly = true)
    public CartItemListResponse getCartItems(Long memberId){
        return cartService.getCartItems(memberId);
    }

    @Transactional
    public CartItemResponse addCartItem(Long memberId, CartItemAddRequest request){
        return cartService.addCartItem(memberId, request);
    }
}
