package com.thock.back.api.boundedContext.market.out;

import com.thock.back.api.boundedContext.market.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
