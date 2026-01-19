package com.thock.back.api.boundedContext.market.out;

import com.thock.back.api.boundedContext.market.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

}
