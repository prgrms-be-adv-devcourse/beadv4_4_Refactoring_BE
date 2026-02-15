package com.thock.back.market.app;

import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderCancelHistory;
import com.thock.back.market.domain.OrderState;
import com.thock.back.market.out.repository.OrderCancelHistoryRepository;
import com.thock.back.market.out.repository.OrderRepository;
import com.thock.back.shared.market.domain.CancelReasonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutScheduler {

    private final OrderRepository orderRepository;
    private final OrderCancelHistoryRepository orderCancelHistoryRepository;

    // TODO : Policy로 변경
    private static final int PAYMENT_TIMEOUT_MINUTES = 30;

    /**
     * 미결제 주문 타임아웃 처리
     * 결제 요청 후 30분 경과한 주문을 자동으로 취소
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);

        List<Order> expiredOrders = orderRepository.findByStateAndRequestPaymentDateBefore(
                OrderState.PENDING_PAYMENT, expiredTime);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("⏰ 타임아웃 주문 처리 시작: {}건", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // 주문 취소 (결제 요청 중 취소)
                order.cancelRequestPayment(CancelReasonType.PAYMENT_TIMEOUT, null);

                // 히스토리 저장 (시스템 취소)
                List<OrderCancelHistory> histories = order.getItems().stream()
                        .map(item -> OrderCancelHistory.ofSystemCancel(order, item, CancelReasonType.PAYMENT_TIMEOUT))
                        .toList();
                orderCancelHistoryRepository.saveAll(histories);

                log.info("⏰ 타임아웃 주문 취소 완료: orderId={}, orderNumber={}",
                        order.getId(), order.getOrderNumber());

            } catch (Exception e) {
                log.error("⏰ 타임아웃 주문 취소 실패: orderId={}, error={}",
                        order.getId(), e.getMessage());
            }
        }

        log.info("⏰ 타임아웃 주문 처리 완료: {}건", expiredOrders.size());
    }
}
