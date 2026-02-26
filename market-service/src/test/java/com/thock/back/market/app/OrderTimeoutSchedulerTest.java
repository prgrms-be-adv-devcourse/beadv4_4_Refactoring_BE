package com.thock.back.market.app;

import com.thock.back.market.app.scheduler.OrderTimeoutCancelWorker;
import com.thock.back.market.app.scheduler.OrderTimeoutScheduler;
import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderState;
import com.thock.back.market.out.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutSchedulerTest {

    @InjectMocks
    private OrderTimeoutScheduler orderTimeoutScheduler;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTimeoutCancelWorker orderTimeoutCancelWorker;

    private Order createOrderWithId(Long orderId) throws Exception {
        Order order = mock(Order.class);
        given(order.getId()).willReturn(orderId);
        return order;
    }

    @Nested
    @DisplayName("cancelExpiredOrders 타임아웃 처리 테스트")
    class CancelExpiredOrdersTest {

        @Test
        @DisplayName("30분 경과한 주문은 worker로 취소 위임된다")
        void cancelExpiredOrders_delegatesToWorker() throws Exception {
            Order expiredOrder = createOrderWithId(1L);
            given(orderRepository.findByStateAndRequestPaymentDateBefore(
                    eq(OrderState.PENDING_PAYMENT), any(LocalDateTime.class)))
                    .willReturn(List.of(expiredOrder));

            orderTimeoutScheduler.cancelExpiredOrders();

            verify(orderTimeoutCancelWorker).cancelOne(1L);
        }

        @Test
        @DisplayName("만료된 주문이 없으면 worker가 호출되지 않는다")
        void cancelExpiredOrders_noExpiredOrders() {
            given(orderRepository.findByStateAndRequestPaymentDateBefore(
                    eq(OrderState.PENDING_PAYMENT), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            orderTimeoutScheduler.cancelExpiredOrders();

            verify(orderTimeoutCancelWorker, never()).cancelOne(any());
        }

        @Test
        @DisplayName("여러 만료 주문을 한 번에 처리한다")
        void cancelExpiredOrders_multipleOrders_allProcessed() throws Exception {
            Order expiredOrder1 = createOrderWithId(1L);
            Order expiredOrder2 = createOrderWithId(2L);
            given(orderRepository.findByStateAndRequestPaymentDateBefore(
                    eq(OrderState.PENDING_PAYMENT), any(LocalDateTime.class)))
                    .willReturn(List.of(expiredOrder1, expiredOrder2));

            orderTimeoutScheduler.cancelExpiredOrders();

            verify(orderTimeoutCancelWorker).cancelOne(1L);
            verify(orderTimeoutCancelWorker).cancelOne(2L);
            verify(orderTimeoutCancelWorker, times(2)).cancelOne(any());
        }

        @Test
        @DisplayName("한 주문 취소 실패해도 다른 주문은 계속 처리된다")
        void cancelExpiredOrders_oneFailure_continuesProcessing() throws Exception {
            Order expiredOrder1 = createOrderWithId(1L);
            Order expiredOrder2 = createOrderWithId(2L);
            given(orderRepository.findByStateAndRequestPaymentDateBefore(
                    eq(OrderState.PENDING_PAYMENT), any(LocalDateTime.class)))
                    .willReturn(List.of(expiredOrder1, expiredOrder2));
            doThrow(new RuntimeException("boom")).when(orderTimeoutCancelWorker).cancelOne(1L);

            orderTimeoutScheduler.cancelExpiredOrders();

            verify(orderTimeoutCancelWorker).cancelOne(1L);
            verify(orderTimeoutCancelWorker).cancelOne(2L);
        }
    }
}
