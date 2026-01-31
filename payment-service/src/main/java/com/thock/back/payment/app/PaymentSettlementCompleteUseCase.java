package com.thock.back.payment.app;


import com.thock.back.payment.domain.EventType;
import com.thock.back.payment.domain.Wallet;
import com.thock.back.payment.out.WalletRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentSettlementCompleteUseCase {
    private final WalletRepository walletRepository;
    public void completeSettlementPayment(Long memberID, Long amount) {
        Wallet wallet = walletRepository.findByHolderId(memberID).get();
        wallet.depositRevenue(amount);
        walletRepository.save(wallet);
        wallet.createRevenueLogEvent(amount, EventType.판매수익_입금);
    }
}
