package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.EventType;
import com.thock.back.api.boundedContext.payment.domain.Payment;
import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
import com.thock.back.api.boundedContext.payment.domain.Wallet;
import com.thock.back.api.boundedContext.payment.domain.dto.response.PaymentLogResponseDto;
import com.thock.back.api.boundedContext.payment.domain.dto.response.RevenueLogResponseDto;
import com.thock.back.api.boundedContext.payment.domain.dto.response.WalletLogResponseDto;
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
    private final PaymentCompletedOrderPaymentUseCase paymentCompletedOrderPaymentUseCase;
    private final PaymentFindUseCase paymentFindUseCase;

    @Transactional
    public PaymentMember syncMember(MemberDto member){
        return paymentSyncMemberUseCase.syncMember(member);
    }

    @Transactional(readOnly = true)
    public Optional<Wallet> findWalletByHolder(PaymentMember holder) {
        return paymentSupport.findWalletByHolder(holder);
    }
    @Transactional
    public void requestedOrderPayment(OrderDto order, Long pgPaymentAmount) {
        paymentRequestedOrderPaymentUseCase.requestedOrderPayment(order, pgPaymentAmount);
    }

    // PG사 안 거치고 바로 예치금에서 해결 가능할 때
    @Transactional
    public void completedOrderPayment(OrderDto order) {
        paymentCompletedOrderPaymentUseCase.completedOrderPayment(order);
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

    @Transactional(readOnly = true)
    public WalletDto walletFindByMemberId(Long memberId) {
        return paymentFindUseCase.walletFindByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public WalletLogResponseDto getWalletLog(Long memberId) {
        return paymentFindUseCase.getWalletLog(memberId);
    }

    @Transactional(readOnly = true)
    public PaymentLogResponseDto getPaymentLog(Long memberId) {
        return paymentFindUseCase.getPaymentLog(memberId);
    }

    @Transactional(readOnly = true)
    public RevenueLogResponseDto getRevenueLog(Long memberId) {
        return paymentFindUseCase.getRevenueLog(memberId);
    }
}
