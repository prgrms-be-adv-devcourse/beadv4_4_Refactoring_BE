package com.thock.back.market.app.scheduler;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderCancelHistory;
import com.thock.back.market.out.repository.OrderCancelHistoryRepository;
import com.thock.back.market.out.repository.OrderRepository;
import com.thock.back.shared.market.domain.CancelReasonType;
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
public class OrderTimeoutCancelWorker {

    private final OrderRepository orderRepository;
    private final OrderCancelHistoryRepository orderCancelHistoryRepository;

    /**
     * 주문 1건만 별도 트랜잭션으로 처리한다.
     * - REQUIRES_NEW: 바깥 루프 트랜잭션과 분리
     * - 효과: 한 건 실패/충돌이 다른 주문 처리에 전파되지 않음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelOne(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        try {
            // 결제 대기 상태 주문만 타임아웃 취소
            order.cancelRequestPayment(CancelReasonType.PAYMENT_TIMEOUT, null);

            // 시스템 취소 히스토리 저장
            List<OrderCancelHistory> histories = order.getItems().stream()
                    .map(item -> OrderCancelHistory.ofSystemCancel(order, item, CancelReasonType.PAYMENT_TIMEOUT))
                    .toList();
            orderCancelHistoryRepository.saveAll(histories);

            // DB 반영을 즉시 시도해 충돌을 이 메서드 안에서 감지
            orderRepository.flush();

            log.info("⏰ 타임아웃 주문 취소 완료: orderId={}, orderNumber={}",
                    order.getId(), order.getOrderNumber());
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            // 스케줄러 vs 사용자 액션(결제완료/취소) 경합 시 해당 건만 스킵
            log.warn("⏰ 타임아웃 주문 충돌 감지(스킵): orderId={}, orderNumber={}",
                    order.getId(), order.getOrderNumber());
        } catch (CustomException e) {
            // 이미 상태가 바뀌어 취소 불가해진 경우도 스킵 로그로 처리
            if (e.getErrorCode() == ErrorCode.ORDER_CANNOT_CANCEL || e.getErrorCode() == ErrorCode.ORDER_INVALID_STATE) {
                log.warn("⏰ 타임아웃 처리 스킵(이미 상태 변경): orderId={}, orderNumber={}, code={}",
                        order.getId(), order.getOrderNumber(), e.getErrorCode().getCode());
                return;
            }
            throw e;
        }
    }
}
