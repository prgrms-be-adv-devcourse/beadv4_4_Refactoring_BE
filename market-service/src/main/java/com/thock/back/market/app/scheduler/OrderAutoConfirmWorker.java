package com.thock.back.market.app.scheduler;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.domain.Order;
import com.thock.back.market.out.repository.OrderRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAutoConfirmWorker {

    private final OrderRepository orderRepository;

    /**
     * 주문 1건만 별도 트랜잭션으로 자동 구매 확정한다.
     * - REQUIRES_NEW: 한 건 실패가 다른 주문 처리에 전파되지 않음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmOne(Long orderId, List<Long> orderItemIds) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        try {
            // 부분/전체 모두 confirmItems 경로로 일관 처리
            order.confirmItems(orderItemIds);
            orderRepository.flush();

            log.info("⏰ 자동 구매확정 완료: orderId={}, orderNumber={}, itemCount={}",
                    order.getId(), order.getOrderNumber(), orderItemIds.size());
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            log.warn("⏰ 자동 구매확정 충돌 감지(스킵): orderId={}, orderNumber={}",
                    order.getId(), order.getOrderNumber());
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.ORDER_INVALID_STATE
                    || e.getErrorCode() == ErrorCode.ORDER_ITEM_NOT_FOUND) {
                log.warn("⏰ 자동 구매확정 스킵(이미 상태 변경): orderId={}, orderNumber={}, code={}",
                        order.getId(), order.getOrderNumber(), e.getErrorCode().getCode());
                return;
            }
            throw e;
        }
    }
}
