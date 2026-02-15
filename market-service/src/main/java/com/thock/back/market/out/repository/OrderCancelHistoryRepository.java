package com.thock.back.market.out.repository;

import com.thock.back.market.domain.OrderCancelHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderCancelHistoryRepository extends JpaRepository<OrderCancelHistory, Long> {
    List<OrderCancelHistory> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<OrderCancelHistory> findByOrderItemIdOrderByCreatedAtDesc(Long orderItemId);
}
