package com.thock.back.product.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOutboxEventRepository extends JpaRepository<ProductOutboxEvent, Long> {
    List<ProductOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(ProductOutboxStatus status);
}
