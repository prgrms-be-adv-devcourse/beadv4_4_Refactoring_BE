package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.*;
import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository;
import com.thock.back.api.boundedContext.payment.out.PaymentRepository;
import com.thock.back.api.boundedContext.payment.out.WalletRepository;
import com.thock.back.api.shared.market.dto.OrderDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentRequestedOrderPaymentUseCase {
    private final PaymentMemberRepository paymentMemberRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;

    public void requestedOrderPayment(OrderDto order) {
        PaymentMember buyer = paymentMemberRepository.findById(order.getBuyerId())
                .orElseThrow();

        Wallet wallet = walletRepository.findByHolder(buyer)
                .orElseThrow();
        Long pgAmount = order.getPrice() - wallet.getBalance();

        Payment payment = new Payment();

        payment.createRequested(
                order.getPrice(),
                order.getOrderId(),
                PaymentStatus.REQUESTED,
                buyer,
                pgAmount > 0 ? pgAmount : 0L
        );

        if(pgAmount <= 0L){
            // 돈 충분함 잔액에서 빼고 바로 결제 가능 토스페이먼츠 필요없음
            wallet.withdrawBalance(order.getPrice(), EventType.주문_출금);
            payment.updatePaymentStatus(PaymentStatus.COMPLETED);
        }
        else{
            // TODO: 토스페이먼츠로 보냄

        }

        paymentRepository.save(payment);
    }
}
