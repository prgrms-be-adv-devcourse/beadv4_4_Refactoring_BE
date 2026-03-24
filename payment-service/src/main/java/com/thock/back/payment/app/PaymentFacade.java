package com.thock.back.payment.app;


import com.thock.back.payment.domain.EventType;
import com.thock.back.payment.domain.PaymentMember;
import com.thock.back.payment.domain.Wallet;
import com.thock.back.payment.domain.dto.request.PaymentConfirmRequestDto;
import com.thock.back.payment.domain.dto.response.PaymentLogResponseDto;
import com.thock.back.payment.domain.dto.response.RevenueLogResponseDto;
import com.thock.back.payment.domain.dto.response.WalletLogResponseDto;
import com.thock.back.payment.out.PaymentRepository;
import com.thock.back.shared.common.dto.DefaultResponseDto;
import com.thock.back.shared.market.dto.OrderDto;
import com.thock.back.shared.member.dto.MemberDto;
import com.thock.back.shared.payment.dto.PaymentCancelRequestDto;
import com.thock.back.shared.payment.dto.PaymentDto;
import com.thock.back.shared.payment.dto.WalletDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
    private final PaymentSyncMemberUseCase paymentSyncMemberUseCase;
    private final PaymentSupport paymentSupport;
    private final PaymentCreateLogUseCase paymentCreateLogUseCase;
    private final PaymentRequestedOrderPaymentUseCase paymentRequestedOrderPaymentUseCase;
    private final PaymentCompletedOrderPaymentUseCase paymentCompletedOrderPaymentUseCase;
    private final PaymentSettlementCompleteUseCase paymentSettlementCompleteUseCase;
    private final PaymentRepository paymentRepository;
    private final PaymentFindUseCase paymentFindUseCase;
    private final PaymentConfirmAndRefundUseCase paymentConfirmAndRefundUseCase;
    private final PaymentAccountWithdrawUseCase paymentAccountWithdrawUseCase;

    @Transactional
    public PaymentMember syncMember(MemberDto member){
        return paymentSyncMemberUseCase.syncMember(member);
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

    // 취소 이벤트 발행되면 토스 결제인지 일반 결제인지 체크하는 곳
    @Transactional
    public void canceledPayment(PaymentCancelRequestDto dto) {
        Long pgAmount = paymentRepository.findByOrderId(dto.orderId()).get().getPgAmount();
        if(pgAmount > 0) { // 토스페이먼츠 결제
            this.canceledOrderTossPayment(dto);
            return;
        }
        this.canceledOrderPayment(dto);
    }

    @Transactional
    public void canceledOrderPayment(PaymentCancelRequestDto dto) {
        paymentConfirmAndRefundUseCase.cancelPayment(dto);
    }

    @Transactional
    public void canceledOrderTossPayment(PaymentCancelRequestDto dto) {
        paymentConfirmAndRefundUseCase.cancelToss(dto);
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

    @Transactional
    public void completeSettlementPayment(Long memberID, Long amount) {
        paymentSettlementCompleteUseCase.completeSettlementPayment(memberID, amount);
    }

    @Transactional
    public DefaultResponseDto accountWithdraw(Long memberId, Long amount) {
        return paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount);
    }

    @Transactional
    public Map<String, Object> confirmPayment(PaymentConfirmRequestDto request) {
        return paymentConfirmAndRefundUseCase.confirmPayment(request);
    }

    @Transactional
    public void canceledBeforePayment(String orderId) {
        paymentConfirmAndRefundUseCase.cancelBeforePayment(orderId);
    }
}
