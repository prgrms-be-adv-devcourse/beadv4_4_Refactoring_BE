package com.thock.back.api.boundedContext.market.app;

import com.thock.back.api.boundedContext.market.domain.Cart;
import com.thock.back.api.boundedContext.market.domain.MarketMember;
import com.thock.back.api.boundedContext.market.out.repository.CartRepository;
import com.thock.back.api.boundedContext.market.out.repository.MarketMemberRepository;
import com.thock.back.api.shared.market.dto.MarketMemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketCreateCartUseCase {
    private final MarketMemberRepository marketMemberRepository;
    private final CartRepository cartRepository;

    @Transactional
    public Cart createCart(MarketMemberDto buyer) {
        MarketMember _buyer = marketMemberRepository.getReferenceById(buyer.getId());

        Cart cart = new Cart(_buyer);

        return cartRepository.save(cart);
    }
}
