package com.thock.back.market.app.scheduler;

import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderItem;
import com.thock.back.market.domain.OrderItemState;
import com.thock.back.market.domain.OrderState;
import com.thock.back.market.out.repository.OrderItemRepository;
import com.thock.back.market.out.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoConfirmScheduler {
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    @Value("${market.order.auto-confirm-days:7}")
    private int autoConfirmDays;

    /**
     * 자동 구매 확정 처리
     * 배송 완료 후 7일이 지난 제품을 구매 확정 처리
     */
    @Scheduled(cron = "${market.order.auto-confirm-cron:0 0 3 * * *}") // 기본: 매일 새벽 3시 실행
    @Transactional
    public void autoConfirmDeliveredOrders() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(autoConfirmDays);

        List<OrderItem> expiredOrderItems = orderItemRepository.findByStateAndDeliveredAtBefore(
                OrderItemState.DELIVERED, cutoffDate
        );

        if (expiredOrderItems.isEmpty()){
            return;
        }

        log.info("⏰ 자동 구매확정 처리 시작: {}건", expiredOrderItems.size());

        // Order별로 그룹핑
        Map<Order, List<OrderItem>> orderItemsMap = expiredOrderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrder));

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<Order, List<OrderItem>> entry : orderItemsMap.entrySet()) {
            Order order = entry.getKey();
            List<OrderItem> items = entry.getValue();

            try {
                // 각 아이템 구매 확정
                items.forEach(OrderItem::confirm);

                // Order 상태 업데이트
                order.updateStateFromItems();

                // 전체 구매확정이면 정산 이벤트 발행
                if (order.getState() == OrderState.CONFIRMED) {
                    order.confirm();
                }

                orderRepository.save(order);
                successCount += items.size();

                log.info("⏰ 자동 구매확정 완료: orderId={}, orderNumber={}, itemCount={}",
                        order.getId(), order.getOrderNumber(), items.size());
            } catch (Exception e) {
                failCount += items.size();
                log.error("⏰ 자동 구매확정 실패: orderId={}, error={}",
                        order.getId(), e.getMessage());
            }

            log.info("⏰ 자동 구매확정 처리 완료: 성공={}건, 실패={}건", successCount, failCount);
        }
    }
}
