package com.thock.back.market.app;

import com.thock.back.shared.market.dto.MarketMemberDto;
import com.thock.back.market.domain.Cart;
import com.thock.back.market.domain.MarketMember;
import com.thock.back.market.out.repository.CartRepository;
import com.thock.back.market.out.repository.MarketMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
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
