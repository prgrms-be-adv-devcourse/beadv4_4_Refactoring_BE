package com.thock.back.api.boundedContext.payment.out;

import com.thock.back.api.boundedContext.payment.domain.WalletLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletLogRepository extends JpaRepository<WalletLog,Long> {
    Optional<WalletLog> findByWalletId(Long walletId);
}
