package com.thock.back.market.app;

import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.domain.Order;
import com.thock.back.market.out.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketCancelOrderPaymentUseCase {
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void cancelOrder(Long memberId, Long orderId){
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 본인 주문인지 확인
        if (!order.getBuyer().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.ORDER_USER_FORBIDDEN);
        }

        // 3. 도메인 메서드 호출 (이벤트 발행은 도메인이 처리)
        if (order.isPaymentInProgress()) {
            order.cancelRequestPayment();
        } else {
            order.cancel();
        }
    }

    // 부분 취소
    @Transactional
    public void cancelOrderItem(Long memberId, Long orderId, Long orderItemId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 본인 주문인지 확인
        if (!order.getBuyer().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.USER_FORBIDDEN);
        }

        // 3. 도메인 메서드 호출 (이벤트 발행은 도메인이 처리)
        order.cancelItem(orderItemId);
    }
}
