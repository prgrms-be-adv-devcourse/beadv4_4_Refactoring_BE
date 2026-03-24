package com.thock.back.market.out.repository;


import com.thock.back.market.domain.Cart;
import com.thock.back.market.domain.MarketMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByBuyer(MarketMember buyer);
}
