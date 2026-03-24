package com.thock.back.market.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.domain.Order;
import com.thock.back.market.out.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketConfirmOrderUseCase {

    private final OrderRepository orderRepository;

    @Transactional
    public void confirmOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getBuyer().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        order.confirm();
        orderRepository.save(order);

        log.info("전체 구매 확정 완료: memberId={}, orderId={}", memberId, orderId);
    }

    @Transactional
    public void confirmOrderItems(Long memberId, Long orderId, List<Long> orderItemIds) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getBuyer().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        order.confirmItems(orderItemIds);
        orderRepository.save(order);

        log.info("부분 구매 확정 완료: memberId={}, orderId={}, itemCount={}", memberId, orderId, orderItemIds.size());
    }
}
