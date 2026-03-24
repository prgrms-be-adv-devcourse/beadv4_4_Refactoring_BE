package com.thock.back.payment.app;


import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.payment.domain.EventType;
import com.thock.back.payment.domain.Wallet;
import com.thock.back.payment.out.WalletRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentSettlementCompleteUseCase {
    private final WalletRepository walletRepository;

    public void completeSettlementPayment(Long memberID, Long amount) {
        Wallet wallet = walletRepository.findByHolderId(memberID)
                .orElseThrow(() -> {
                    log.error("지갑 조회 실패 - memberId={}", memberID);
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });

        wallet.depositRevenue(amount);
        walletRepository.save(wallet);
        wallet.createRevenueLogEvent(amount, EventType.판매수익_입금);
        log.info("정산 완료 - memberId={}, amount={}", memberID, amount);
    }
}
