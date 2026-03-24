package com.thock.back.market.app.scheduler;

import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderState;
import com.thock.back.market.out.repository.OrderRepository;
import com.thock.back.shared.market.dto.OrderDeleteRequestDto;
import com.thock.back.shared.market.event.MarketOrderDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupScheduler {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${market.order.cleanup-retention-days:30}")
    private int cleanupRetentionDays;

    /**
     * 취소된 주문 정리 (30일 경과 후 삭제)
     * CANCELLED 상태 = 실제로 결제가 완료되지 않은 주문
     * Payment는 REQUESTED 혹은 PG_PENDING 상태로 남아있음
     */
    @Scheduled(cron = "${market.order.cleanup-cron:0 0 3 * * *}") // 기본: 매일 새벽 3시 실행
    @Transactional
    public void cleanupCancelledOrders() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupRetentionDays);

        List<Order> expiredOrders = orderRepository.findByStateAndCancelDateBefore(
                OrderState.CANCELLED, cutoffDate);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("🗑️ 취소 주문 정리 시작: {}건", expiredOrders.size());

        int successCount = 0;
        int failCount = 0;

        for (Order order : expiredOrders) {
            try {
                // Payment 모듈에 삭제 알림 이벤트 발행
                eventPublisher.publishEvent(
                        new MarketOrderDeletedEvent(new OrderDeleteRequestDto(order.getOrderNumber()))
                );

                // 주문 삭제
                orderRepository.delete(order);

                successCount++;
                log.debug("🗑️ 주문 삭제 완료: orderId={}, orderNumber={}",
                        order.getId(), order.getOrderNumber());

            } catch (Exception e) {
                failCount++;
                log.error("🗑️ 주문 삭제 실패: orderId={}, error={}",
                        order.getId(), e.getMessage());
            }
        }

        log.info("🗑️ 취소 주문 정리 완료: 성공={}건, 실패={}건", successCount, failCount);
    }
}
