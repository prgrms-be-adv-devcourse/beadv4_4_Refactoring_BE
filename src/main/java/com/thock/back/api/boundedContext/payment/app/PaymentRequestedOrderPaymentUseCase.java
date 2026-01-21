package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.Payment;
import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
import com.thock.back.api.boundedContext.payment.domain.PaymentStatus;
import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository;
import com.thock.back.api.boundedContext.payment.out.PaymentRepository;
import com.thock.back.api.shared.market.dto.OrderDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentRequestedOrderPaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final PaymentMemberRepository paymentMemberRepository;

    public void requestedOrderPayment(OrderDto order, Long pgPaymentAmount, String paymentKey) {
        PaymentMember member = paymentMemberRepository.getReferenceById(order.getBuyerId());

        Payment payment = paymentRepository.save(
            new Payment(
                    pgPaymentAmount,
                    member,
                    PaymentStatus.REQUESTED,
                    order.getOrderNumber(),
                    order.getTotalSalePrice(),
                    paymentKey
            )
        );
    }

}
