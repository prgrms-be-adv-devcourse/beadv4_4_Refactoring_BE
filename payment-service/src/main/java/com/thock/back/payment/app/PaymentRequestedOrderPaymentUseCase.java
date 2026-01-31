package com.thock.back.payment.app;


import com.thock.back.payment.domain.Payment;
import com.thock.back.payment.domain.PaymentMember;
import com.thock.back.payment.domain.PaymentStatus;
import com.thock.back.payment.out.PaymentMemberRepository;
import com.thock.back.payment.out.PaymentRepository;
import com.thock.back.shared.market.dto.OrderDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentRequestedOrderPaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final PaymentMemberRepository paymentMemberRepository;

    public void requestedOrderPayment(OrderDto order, Long pgPaymentAmount) {
        PaymentMember member = paymentMemberRepository.getReferenceById(order.getBuyerId());

        Payment payment = paymentRepository.save(
            new Payment(
                    pgPaymentAmount,
                    member,
                    PaymentStatus.REQUESTED,
                    order.getOrderNumber(),
                    order.getTotalSalePrice(),
                    ""
            )
        );
        payment.createPaymentLogEvent();
    }

}
