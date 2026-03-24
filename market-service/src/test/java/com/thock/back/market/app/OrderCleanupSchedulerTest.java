package com.thock.back.market.app;

import com.thock.back.market.app.scheduler.OrderCleanupScheduler;
import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderState;
import com.thock.back.market.out.repository.OrderRepository;
import com.thock.back.shared.market.event.MarketOrderDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCleanupSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderCleanupScheduler orderCleanupScheduler;

    private Order cancelledOrder;

    @BeforeEach
    void setUp() {
        cancelledOrder = mock(Order.class);
    }

    @Test
    @DisplayName("30일 지난 CANCELLED 주문을 삭제하고 이벤트를 발행한다")
    void cleanupCancelledOrders_deletesExpiredOrdersAndPublishesEvent() {
        // given
        when(cancelledOrder.getId()).thenReturn(1L);
        when(cancelledOrder.getOrderNumber()).thenReturn("ORDER-20250101-ABC123");
        when(orderRepository.findByStateAndCancelDateBefore(
                eq(OrderState.CANCELLED), any(LocalDateTime.class)))
                .thenReturn(List.of(cancelledOrder));

        // when
        orderCleanupScheduler.cleanupCancelledOrders();

        // then
        verify(orderRepository).delete(cancelledOrder);

        ArgumentCaptor<MarketOrderDeletedEvent> eventCaptor =
                ArgumentCaptor.forClass(MarketOrderDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        MarketOrderDeletedEvent event = eventCaptor.getValue();
        assertThat(event.dto().orderNumber()).isEqualTo("ORDER-20250101-ABC123");
    }

    @Test
    @DisplayName("삭제할 주문이 없으면 아무 작업도 하지 않는다")
    void cleanupCancelledOrders_noOrdersToDelete() {
        // given
        when(orderRepository.findByStateAndCancelDateBefore(
                eq(OrderState.CANCELLED), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // when
        orderCleanupScheduler.cleanupCancelledOrders();

        // then
        verify(orderRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("여러 주문을 삭제할 때 각각 이벤트를 발행한다")
    void cleanupCancelledOrders_multipleOrders() {
        // given
        Order order1 = mock(Order.class);
        Order order2 = mock(Order.class);
        when(order1.getId()).thenReturn(1L);
        when(order1.getOrderNumber()).thenReturn("ORDER-001");
        when(order2.getId()).thenReturn(2L);
        when(order2.getOrderNumber()).thenReturn("ORDER-002");

        when(orderRepository.findByStateAndCancelDateBefore(
                eq(OrderState.CANCELLED), any(LocalDateTime.class)))
                .thenReturn(List.of(order1, order2));

        // when
        orderCleanupScheduler.cleanupCancelledOrders();

        // then
        verify(orderRepository).delete(order1);
        verify(orderRepository).delete(order2);
        verify(eventPublisher, times(2)).publishEvent(any(MarketOrderDeletedEvent.class));
    }

    @Test
    @DisplayName("삭제 중 예외 발생 시 다른 주문은 계속 처리한다")
    void cleanupCancelledOrders_continuesOnException() {
        // given
        Order order1 = mock(Order.class);
        Order order2 = mock(Order.class);
        when(order1.getId()).thenReturn(1L);
        when(order1.getOrderNumber()).thenReturn("ORDER-001");
        when(order2.getId()).thenReturn(2L);
        when(order2.getOrderNumber()).thenReturn("ORDER-002");

        when(orderRepository.findByStateAndCancelDateBefore(
                eq(OrderState.CANCELLED), any(LocalDateTime.class)))
                .thenReturn(List.of(order1, order2));

        // order1 삭제 시 예외 발생
        doThrow(new RuntimeException("DB error")).when(orderRepository).delete(order1);

        // when
        orderCleanupScheduler.cleanupCancelledOrders();

        // then
        verify(orderRepository).delete(order1);
        verify(orderRepository).delete(order2);
        // order2는 정상 삭제되므로 이벤트 발행됨
        verify(eventPublisher, times(2)).publishEvent(any(MarketOrderDeletedEvent.class));
    }
}
