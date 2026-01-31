package com.thock.back.market.out.repository;

import com.thock.back.market.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderName);
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
}
