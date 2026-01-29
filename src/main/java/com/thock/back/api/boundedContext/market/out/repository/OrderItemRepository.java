package com.thock.back.api.boundedContext.market.out.repository;

import com.thock.back.api.boundedContext.market.domain.OrderItem;
import com.thock.back.api.boundedContext.market.domain.OrderItemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 정산 대상 조회 (최적화)
     */
    @Query("""
        SELECT oi FROM OrderItem oi
        WHERE oi.sellerId = :sellerId
          AND oi.state = :state
          AND DATE(oi.updatedAt) = :date
        ORDER BY oi.id ASC
        """)
    List<OrderItem> findBySellerIdAndStatusAndDate(
            Long sellerId,
            OrderItemState state,
            LocalDate date
    );
}
