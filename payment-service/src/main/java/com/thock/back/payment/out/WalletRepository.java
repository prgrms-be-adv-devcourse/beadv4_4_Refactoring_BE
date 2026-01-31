package com.thock.back.payment.out;

import com.thock.back.payment.domain.PaymentMember;
import com.thock.back.payment.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet,Long> {
    Optional<Wallet> findByHolder(PaymentMember holder);
    Optional<Wallet> findByHolderId(Long holderId);
}
