package com.thock.back.api.boundedContext.payment.out;

import com.thock.back.api.boundedContext.payment.domain.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {
    Optional<PaymentLog> findByBuyerId(Long buyerId);
    Optional<PaymentLog> findByOrderId(Long orderId);
}
