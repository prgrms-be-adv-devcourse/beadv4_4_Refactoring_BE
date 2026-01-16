package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
import com.thock.back.api.boundedContext.payment.domain.Wallet;
import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository;
import com.thock.back.api.boundedContext.payment.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentSupport {
    private final PaymentMemberRepository paymentMemberRepository;
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public Optional<PaymentMember> findByName(String name){
        return paymentMemberRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public Optional<Wallet> findWalletByHolder(PaymentMember holder) {
        return walletRepository.findByHolder(holder);
    }

    @Transactional(readOnly = true)
    public Optional<Wallet> findWalletByHolderId(int holderId) {
        return walletRepository.findByHolderId(holderId);
    }

}
