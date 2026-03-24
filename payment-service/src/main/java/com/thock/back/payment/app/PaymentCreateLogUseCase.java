package com.thock.back.payment.app;


import com.thock.back.payment.domain.*;
import com.thock.back.payment.out.*;
import com.thock.back.shared.payment.dto.PaymentDto;
import com.thock.back.shared.payment.dto.WalletDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PaymentCreateLogUseCase {
    private final PaymentLogRepository paymentLogRepository;
    private final WalletLogRepository walletLogRepository;
    private final RevenueLogRepository revenueLogRepository;
    private final PaymentMemberRepository paymentMemberRepository;
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public void saveBalanceLog(WalletDto wallet, EventType eventType, Long amount){
        PaymentMember member = paymentMemberRepository.getReferenceById(wallet.holderId());
        Wallet _wallet = walletRepository.getReferenceById(wallet.id());

        walletLogRepository.save(
                new WalletLog(
                        member,
                        _wallet,
                        eventType,
                        amount,
                        wallet.balance())
        );
    }

    @Transactional
    public void saveRevenueLog(WalletDto wallet, EventType eventType, Long amount){
        PaymentMember member = paymentMemberRepository.getReferenceById(wallet.holderId());
        Wallet _wallet = walletRepository.getReferenceById(wallet.id());

        revenueLogRepository.save(
                new RevenueLog(
                        member,
                        _wallet,
                        eventType,
                        amount,
                        wallet.revenue())
        );
    }

    @Transactional
    public void savePaymentLog(PaymentDto payment) {
        PaymentMember member = paymentMemberRepository.getReferenceById(payment.buyerId());
        Payment _payment = paymentRepository.getReferenceById(payment.id());
        paymentLogRepository.save(
                new PaymentLog(
                        member,
                        payment.orderId(),
                        _payment.getStatus(),
                        payment.amount(),
                        payment.pgAmount(),
                        _payment)
        );

    }
}