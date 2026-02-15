package com.thock.back.market.app;

import com.thock.back.global.config.GlobalConfig;
import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.market.domain.*;
import com.thock.back.market.out.repository.OrderCancelHistoryRepository;
import com.thock.back.market.out.repository.OrderRepository;
import com.thock.back.shared.market.domain.CancelReasonType;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutSchedulerTest {

    @InjectMocks
    private OrderTimeoutScheduler orderTimeoutScheduler;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderCancelHistoryRepository orderCancelHistoryRepository;

    @Captor
    private ArgumentCaptor<List<OrderCancelHistory>> historyCaptor;

    private MarketMember buyer;

    @BeforeEach
    void setUp() throws Exception {
        MarketPolicy.PRODUCT_PAYOUT_RATE = 80.0;

        buyer = new MarketMember(
                "test@test.com",
                "테스트유저",
                MemberRole.USER,
                MemberState.ACTIVE,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // Mock EventPublisher 설정
        EventPublisher mockEventPublisher = mock(EventPublisher.class);
        Field eventPublisherField = GlobalConfig.class.getDeclaredField("eventPublisher");
        eventPublisherField.setAccessible(true);
        eventPublisherField.set(null, mockEventPublisher);
    }

    private Order createOrderWithItems(Long orderId, int itemCount) throws Exception {
        Order order = new Order(buyer, "12345", "서울시 강남구", "101호");
        setEntityId(order, orderId);

        // 결제 요청 중 상태로 설정 (35분 전 - 타임아웃 대상)
        Field requestPaymentDateField = Order.class.getDeclaredField("requestPaymentDate");
        requestPaymentDateField.setAccessible(true);
        requestPaymentDateField.set(order, LocalDateTime.now().minusMinutes(35));

        for (int i = 1; i <= itemCount; i++) {
            OrderItem item = order.addItem(
                    1L, (long) (100 + i), "상품" + i, "http://image.url",
                    10000L, 9000L, 1
            );
            setEntityId(item, (long) i);
        }

        return order;
    }

    private void setEntityId(Object entity, Long id) throws Exception {
        Class<?> clazz = entity.getClass();
        while (clazz != null) {
            try {
                Field idField = clazz.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("id field not found");
    }

    @Nested
    @DisplayName("cancelExpiredOrders 타임아웃 처리 테스트")
    class CancelExpiredOrdersTest {

        @Test
        @DisplayName("30분 경과한 주문이 취소된다")
        void cancelExpiredOrders_cancelsExpiredOrder() throws Exception {
            // given
            Order expiredOrder = createOrderWithItems(1L, 2);
            given(orderRepository.findByStateAndRequestPaymentDateBefore(
                    eq(OrderState.PENDING_PAYMENT), any(LocalDateTime.class)))
                    .willReturn(List.of(expiredOrder));

            // when
            orderTimeoutScheduler.cancelExpiredOrders();

            // then
            assertThat(expiredOrder.getState()).isEqualTo(OrderState.CANCELLED);
            expiredOrder.getItems().forEach(item ->
                    assertThat(item.getState()).isEqualTo(OrderItemState.CANCELLED)
            );
        }

        @Test
        @DisplayName("타임아웃 취소 시 OrderCancelHistory가 각 OrderItem별로 저장된다")
        void cancelExpiredOrders_savesHistoryPerOrderItem() throws Exception {
            // given
            Order expiredOrder = createOrderWithItems(1L, 3); // 3개 아이템
            given(orderRepository.findByStateAndRequestPaymentDateBefore(
                    eq(OrderState.PENDING_PAYMENT), any(LocalDateTime.class)))
                    .willReturn(List.of(expiredOrder));

            // when
            orderTimeoutScheduler.cancelExpiredOrders();

            // then
            verify(orderCancelHistoryRepository).saveAll(historyCaptor.capture());
            List<OrderCancelHistory> savedHistories = historyCaptor.getValue();

            assertThat(savedHistories).hasSize(3); // 3개 아이템 = 3개 히스토리
            savedHistories.forEach(history -> {
                assertThat(history.getCancelReasonType()).isEqualTo(CancelReasonType.PAYMENT_TIMEOUT);
                assertThat(history.getCancelledBy()).isEqualTo(OrderCancelHistory.CancelledBy.SYSTEM);
            });
        }

        @Test
        @DisplayName("만료된 주문이 없으면 히스토리 저장이 호출되지 않는다")
        void cancelExpiredOrders_noExpiredOrders_doesNotSaveHistory() {
            // given
            given(orderRepository.findByStateAndRequestPaymentDateBefore(
                    eq(OrderState.PENDING_PAYMENT), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // when
            orderTimeoutScheduler.cancelExpiredOrders();

            // then
            verify(orderCancelHistoryRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("여러 만료 주문을 한 번에 처리한다")
        void cancelExpiredOrders_multipleOrders_allProcessed() throws Exception {
            // given
            Order expiredOrder1 = createOrderWithItems(1L, 2);
            Order expiredOrder2 = createOrderWithItems(2L, 1);

            given(orderRepository.findByStateAndRequestPaymentDateBefore(
                    eq(OrderState.PENDING_PAYMENT), any(LocalDateTime.class)))
                    .willReturn(List.of(expiredOrder1, expiredOrder2));

            // when
            orderTimeoutScheduler.cancelExpiredOrders();

            // then
            assertThat(expiredOrder1.getState()).isEqualTo(OrderState.CANCELLED);
            assertThat(expiredOrder2.getState()).isEqualTo(OrderState.CANCELLED);

            // 각 주문별로 saveAll 호출됨 (2번)
            verify(orderCancelHistoryRepository, times(2)).saveAll(any());
        }

        @Test
        @DisplayName("취소된 OrderItem의 취소 사유가 PAYMENT_TIMEOUT이다")
        void cancelExpiredOrders_itemReasonIsPaymentTimeout() throws Exception {
            // given
            Order expiredOrder = createOrderWithItems(1L, 1);
            given(orderRepository.findByStateAndRequestPaymentDateBefore(
                    eq(OrderState.PENDING_PAYMENT), any(LocalDateTime.class)))
                    .willReturn(List.of(expiredOrder));

            // when
            orderTimeoutScheduler.cancelExpiredOrders();

            // then
            OrderItem cancelledItem = expiredOrder.getItems().get(0);
            assertThat(cancelledItem.getCancelReasonType()).isEqualTo(CancelReasonType.PAYMENT_TIMEOUT);
            assertThat(CancelReasonType.PAYMENT_TIMEOUT.isSystemCancellation()).isTrue();
        }

        @Test
        @DisplayName("한 주문 취소 실패해도 다른 주문은 계속 처리된다")
        void cancelExpiredOrders_oneFailure_continuesProcessing() throws Exception {
            // given
            Order expiredOrder1 = createOrderWithItems(1L, 1);
            Order expiredOrder2 = createOrderWithItems(2L, 1);

            // expiredOrder1을 이미 취소된 상태로 만들어서 예외 발생하게 함
            Field stateField = Order.class.getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(expiredOrder1, OrderState.CANCELLED);

            given(orderRepository.findByStateAndRequestPaymentDateBefore(
                    eq(OrderState.PENDING_PAYMENT), any(LocalDateTime.class)))
                    .willReturn(List.of(expiredOrder1, expiredOrder2));

            // when
            orderTimeoutScheduler.cancelExpiredOrders();

            // then - expiredOrder2는 정상 처리됨
            assertThat(expiredOrder2.getState()).isEqualTo(OrderState.CANCELLED);
        }
    }
}
