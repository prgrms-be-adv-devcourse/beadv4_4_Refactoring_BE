package com.thock.back.payment.out;


import com.thock.back.payment.domain.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {
    List<PaymentLog> findByBuyerId(Long buyerId);
    Optional<PaymentLog> findByOrderId(String orderId);
}
