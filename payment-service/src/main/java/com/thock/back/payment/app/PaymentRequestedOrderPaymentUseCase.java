package com.thock.back.payment.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.payment.domain.Payment;
import com.thock.back.payment.domain.PaymentMember;
import com.thock.back.payment.domain.PaymentStatus;
import com.thock.back.payment.out.PaymentMemberRepository;
import com.thock.back.payment.out.PaymentRepository;
import com.thock.back.shared.market.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentRequestedOrderPaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final PaymentMemberRepository paymentMemberRepository;

    public void requestedOrderPayment(OrderDto order, Long pgPaymentAmount) {
        PaymentMember member = paymentMemberRepository.findById(order.buyerId())
                .orElseThrow(() -> {
                    log.error("멤버 조회 실패 - memberId={}", order.buyerId());
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });

        Payment payment = paymentRepository.save(
            new Payment(
                    pgPaymentAmount,
                    member,
                    PaymentStatus.REQUESTED,
                    order.orderNumber(),
                    order.totalSalePrice(),
                    ""
            )
        );
        payment.createPaymentLogEvent();
        log.info("결제 요청 생성 완료 - orderId={}, memberId={}, pgAmount={}, totalAmount={}",
                order.orderNumber(), order.buyerId(), pgPaymentAmount, order.totalSalePrice());
    }

}
