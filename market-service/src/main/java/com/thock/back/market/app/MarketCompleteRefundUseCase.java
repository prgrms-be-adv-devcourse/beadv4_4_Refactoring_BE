package com.thock.back.market.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.domain.Order;
import com.thock.back.market.out.repository.OrderRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketCompleteRefundUseCase {
    private final OrderRepository orderRepository;

    @Retryable(
            retryFor = {
                    OptimisticLockException.class,
                    ObjectOptimisticLockingFailureException.class
            },
            maxAttemptsExpression = "${market.retry.optimistic-lock.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${market.retry.optimistic-lock.backoff-ms:100}")
    )
    @Transactional
    public void completeRefund(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 도메인 메서드 호출
        order.completeRefund();

        log.info("환불 완료 처리: orderId={}, orderNumber={}, state={}",
                order.getId(), orderNumber, order.getState());
    }
}
