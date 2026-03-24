package com.thock.back.payment.out;

import com.thock.back.payment.domain.RevenueLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RevenueLogRepository extends JpaRepository<RevenueLog, Long> {
    List<RevenueLog> findByWalletId(Long walletId);
}
