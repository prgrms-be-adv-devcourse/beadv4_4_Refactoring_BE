package com.thock.back.api.boundedContext.payment.out;

import com.thock.back.api.boundedContext.payment.domain.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {
}
