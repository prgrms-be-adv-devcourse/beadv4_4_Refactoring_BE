package com.thock.back.payment.app;


import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.payment.domain.*;
import com.thock.back.payment.out.PaymentMemberRepository;
import com.thock.back.payment.out.PaymentRepository;
import com.thock.back.payment.out.WalletRepository;
import com.thock.back.shared.market.dto.OrderDto;
import com.thock.back.shared.payment.dto.PaymentDto;
import com.thock.back.shared.payment.event.PaymentCompletedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentCompletedOrderPaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final PaymentMemberRepository paymentMemberRepository;
    private final WalletRepository walletRepository;
    private final EventPublisher eventPublisher;

    public void completedOrderPayment(OrderDto order) {
        Wallet wallet = walletRepository.findByHolderId(order.buyerId())
                .orElseThrow(() -> {
                    log.error("지갑 조회 실패 - orderId={}, memberId={}", order.orderNumber(), order.buyerId());
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });
        PaymentMember member = paymentMemberRepository.findById(order.buyerId())
                .orElseThrow(() -> {
                    log.error("멤버 조회 실패 - orderId={}, memberId={}", order.orderNumber(), order.buyerId());
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
        log.info("일반 결제 신청 요청- orderId={}, Amount={}", order.orderNumber(), order.totalSalePrice());

        if (wallet.getBalance() < order.totalSalePrice()) {
            log.error("잔액 부족 - orderId={}, memberId={}, balance={}, required={}",
                    order.orderNumber(), order.buyerId(), wallet.getBalance(), order.totalSalePrice());
            throw new CustomException(ErrorCode.WALLET_NOT_WITHDRAW);
        }

        wallet.withdrawBalance(order.totalSalePrice());
        wallet.createBalanceLogEvent(order.totalSalePrice(), EventType.주문_출금);
        walletRepository.save(wallet);

        Payment payment = paymentRepository.save(
                new Payment(
                        0L,
                        member,
                        PaymentStatus.COMPLETED,
                        order.orderNumber(),
                        order.totalSalePrice(),
                        ""
                )
        );

        PaymentDto paymentDto = new PaymentDto(payment.getId(),
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getBuyer().getId(),
                payment.getPgAmount(),
                payment.getAmount(),
                payment.getCreatedAt(),
                payment.getRefundedAmount());

        eventPublisher.publish(
                new PaymentCompletedEvent(
                        paymentDto
                )
        );

        log.info("일반 결제 완료 - orderId={}, memberId={}, amount={}", order.orderNumber(), order.buyerId(), order.totalSalePrice());
    }
}
