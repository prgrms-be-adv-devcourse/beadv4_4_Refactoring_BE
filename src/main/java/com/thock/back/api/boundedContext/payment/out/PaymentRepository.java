package com.thock.back.api.boundedContext.payment.out;

import com.thock.back.api.boundedContext.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Optional<Payment> findByOrderId(String orderId);
}
