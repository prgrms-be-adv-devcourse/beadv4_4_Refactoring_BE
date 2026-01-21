package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.EventType;
import com.thock.back.api.boundedContext.payment.domain.Payment;
import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
import com.thock.back.api.boundedContext.payment.domain.Wallet;
import com.thock.back.api.shared.market.dto.OrderDto;
import com.thock.back.api.shared.member.dto.MemberDto;
import com.thock.back.api.shared.payment.dto.PaymentDto;
import com.thock.back.api.shared.payment.dto.WalletDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
    private final PaymentSyncMemberUseCase paymentSyncMemberUseCase;
    private final PaymentSupport paymentSupport;
    private final PaymentCreateLogUseCase paymentCreateLogUseCase;
    private final PaymentRequestedOrderPaymentUseCase paymentRequestedOrderPaymentUseCase;

    @Transactional
    public PaymentMember syncMember(MemberDto member){
        return paymentSyncMemberUseCase.syncMember(member);
    }

    @Transactional(readOnly = true)
    public Optional<Wallet> findWalletByHolder(PaymentMember holder) {
        return paymentSupport.findWalletByHolder(holder);
    }
    @Transactional
    public void requestedOrderPayment(OrderDto order, Long pgPaymentAmount, String paymentKey) {
        paymentRequestedOrderPaymentUseCase.requestedOrderPayment(order, pgPaymentAmount, paymentKey);
    }

    @Transactional
    public void addRevenueLog(WalletDto wallet, EventType eventType, Long amount) {
        paymentCreateLogUseCase.saveRevenueLog(wallet, eventType, amount);
    }

    @Transactional
    public void addBalanceLog(WalletDto wallet, EventType eventType, Long amount) {
        paymentCreateLogUseCase.saveBalanceLog(wallet, eventType, amount);
    }

    @Transactional
    public void addPaymentLog(PaymentDto payment) {
        paymentCreateLogUseCase.savePaymentLog(payment);
    }

}
