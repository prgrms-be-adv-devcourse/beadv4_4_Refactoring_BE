package com.thock.back.api.boundedContext.payment.out;

import com.thock.back.api.boundedContext.payment.domain.RevenueLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RevenueLogRepository extends JpaRepository<RevenueLog, Long> {
    Optional<RevenueLog> findByWalletId(Long walletId);
}
