package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.*;
import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository;
import com.thock.back.api.boundedContext.payment.out.PaymentRepository;
import com.thock.back.api.boundedContext.payment.out.WalletRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import com.thock.back.api.shared.market.dto.OrderDto;
import com.thock.back.api.shared.payment.dto.PaymentDto;
import com.thock.back.api.shared.payment.event.PaymentCompletedEvent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentCompletedOrderPaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final PaymentMemberRepository paymentMemberRepository;
    private final WalletRepository walletRepository;
    private final EventPublisher eventPublisher;

    public void completedOrderPayment(OrderDto order) {
        Wallet wallet = walletRepository.findByHolderId(order.getBuyerId()).get();
        PaymentMember member = paymentMemberRepository.getReferenceById(order.getBuyerId());

        if(wallet.getBalance() >= order.getTotalSalePrice()){
            wallet.withdrawBalance(order.getTotalSalePrice());
            wallet.createBalanceLogEvent(order.getTotalSalePrice(), EventType.주문_출금);
            walletRepository.save(wallet);
        }else return;

        Payment payment = paymentRepository.save(
                new Payment(
                        0L,
                        member,
                        PaymentStatus.COMPLETED,
                        order.getOrderNumber(),
                        order.getTotalSalePrice(),
                        ""
                )
        );

        PaymentDto paymentDto = new PaymentDto(payment.getId(),
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getBuyer().getId(),
                payment.getPgAmount(),
                payment.getAmount(),
                payment.getCreatedAt());

        eventPublisher.publish(
                new PaymentCompletedEvent(
                        paymentDto
                )
        );


    }
}
