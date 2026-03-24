package com.thock.back.market.app;

import com.thock.back.global.config.GlobalConfig;
import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarketCancelOrderPaymentUseCaseTest {

    @InjectMocks
    private MarketCancelOrderPaymentUseCase marketCancelOrderPaymentUseCase;

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
        setEntityId(buyer, 1L);

        // Mock EventPublisher 설정
        EventPublisher mockEventPublisher = mock(EventPublisher.class);
        Field eventPublisherField = GlobalConfig.class.getDeclaredField("eventPublisher");
        eventPublisherField.setAccessible(true);
        eventPublisherField.set(null, mockEventPublisher);
    }

    private Order createOrder(Long orderId, int itemCount) throws Exception {
        Order order = new Order(buyer, "12345", "서울시 강남구", "101호");
        setEntityId(order, orderId);

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

    private void setOrderPaymentInProgress(Order order) throws Exception {
        Field requestPaymentDateField = Order.class.getDeclaredField("requestPaymentDate");
        requestPaymentDateField.setAccessible(true);
        requestPaymentDateField.set(order, LocalDateTime.now());
    }

    @Nested
    @DisplayName("cancelOrder 전체 취소 테스트")
    class CancelOrderTest {

        @Test
        @DisplayName("전체 취소 시 OrderCancelHistory가 각 OrderItem별로 저장된다")
        void cancelOrder_savesHistoryPerOrderItem() throws Exception {
            // given
            Long memberId = 1L;
            Long orderId = 1L;
            Order order = createOrder(orderId, 3); // 3개 아이템
            setOrderPaymentInProgress(order);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            marketCancelOrderPaymentUseCase.cancelOrder(
                    memberId, orderId, CancelReasonType.CHANGE_OF_MIND, "단순 변심"
            );

            // then
            verify(orderCancelHistoryRepository).saveAll(historyCaptor.capture());
            List<OrderCancelHistory> savedHistories = historyCaptor.getValue();

            assertThat(savedHistories).hasSize(3);
            savedHistories.forEach(history -> {
                assertThat(history.getCancelReasonType()).isEqualTo(CancelReasonType.CHANGE_OF_MIND);
                assertThat(history.getCancelReasonDetail()).isEqualTo("단순 변심");
                assertThat(history.getCancelledBy()).isEqualTo(OrderCancelHistory.CancelledBy.USER);
            });
        }

        @Test
        @DisplayName("히스토리에 Order와 OrderItem 참조가 정확히 저장된다")
        void cancelOrder_historyHasCorrectReferences() throws Exception {
            // given
            Long memberId = 1L;
            Long orderId = 1L;
            Order order = createOrder(orderId, 2);
            setOrderPaymentInProgress(order);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            marketCancelOrderPaymentUseCase.cancelOrder(
                    memberId, orderId, CancelReasonType.PRODUCT_DEFECT, null
            );

            // then
            verify(orderCancelHistoryRepository).saveAll(historyCaptor.capture());
            List<OrderCancelHistory> savedHistories = historyCaptor.getValue();

            savedHistories.forEach(history -> {
                assertThat(history.getOrder()).isEqualTo(order);
                assertThat(order.getItems()).contains(history.getOrderItem());
            });
        }

        @Test
        @DisplayName("본인 주문이 아니면 ORDER_USER_FORBIDDEN 예외 발생")
        void cancelOrder_notOwner_throwsException() throws Exception {
            // given
            Long memberId = 999L; // 다른 사용자
            Long orderId = 1L;
            Order order = createOrder(orderId, 1);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() ->
                    marketCancelOrderPaymentUseCase.cancelOrder(
                            memberId, orderId, CancelReasonType.CHANGE_OF_MIND, null
                    ))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customException = (CustomException) ex;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.ORDER_USER_FORBIDDEN);
                    });
        }

        @Test
        @DisplayName("주문이 존재하지 않으면 ORDER_NOT_FOUND 예외 발생")
        void cancelOrder_orderNotFound_throwsException() {
            // given
            Long memberId = 1L;
            Long orderId = 999L;

            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    marketCancelOrderPaymentUseCase.cancelOrder(
                            memberId, orderId, CancelReasonType.CHANGE_OF_MIND, null
                    ))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customException = (CustomException) ex;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("cancelOrderItems 부분 취소 테스트")
    class CancelOrderItemsTest {

        @Test
        @DisplayName("부분 취소 시 취소된 OrderItem만 히스토리에 저장된다")
        void cancelOrderItems_savesHistoryOnlyForCancelledItems() throws Exception {
            // given
            Long memberId = 1L;
            Long orderId = 1L;
            Order order = createOrder(orderId, 3); // 3개 아이템
            List<Long> cancelItemIds = List.of(1L, 2L); // 2개만 취소

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            marketCancelOrderPaymentUseCase.cancelOrderItems(
                    memberId, orderId, cancelItemIds, CancelReasonType.WRONG_OPTION, "옵션 잘못 선택"
            );

            // then
            verify(orderCancelHistoryRepository).saveAll(historyCaptor.capture());
            List<OrderCancelHistory> savedHistories = historyCaptor.getValue();

            assertThat(savedHistories).hasSize(2); // 취소된 2개만 저장
            savedHistories.forEach(history -> {
                assertThat(history.getCancelReasonType()).isEqualTo(CancelReasonType.WRONG_OPTION);
                assertThat(history.getCancelReasonDetail()).isEqualTo("옵션 잘못 선택");
                assertThat(history.getCancelledBy()).isEqualTo(OrderCancelHistory.CancelledBy.USER);
                assertThat(cancelItemIds).contains(history.getOrderItem().getId());
            });
        }

        @Test
        @DisplayName("부분 취소된 OrderItem만 CANCELLED 상태가 된다")
        void cancelOrderItems_onlyCancelledItemsChangeState() throws Exception {
            // given
            Long memberId = 1L;
            Long orderId = 1L;
            Order order = createOrder(orderId, 3);
            List<Long> cancelItemIds = List.of(1L);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            marketCancelOrderPaymentUseCase.cancelOrderItems(
                    memberId, orderId, cancelItemIds, CancelReasonType.CHANGE_OF_MIND, null
            );

            // then
            OrderItem cancelledItem = order.getItems().stream()
                    .filter(item -> item.getId().equals(1L))
                    .findFirst().orElseThrow();
            OrderItem remainingItem1 = order.getItems().stream()
                    .filter(item -> item.getId().equals(2L))
                    .findFirst().orElseThrow();
            OrderItem remainingItem2 = order.getItems().stream()
                    .filter(item -> item.getId().equals(3L))
                    .findFirst().orElseThrow();

            assertThat(cancelledItem.getState()).isEqualTo(OrderItemState.CANCELLED);
            assertThat(remainingItem1.getState()).isEqualTo(OrderItemState.PENDING_PAYMENT);
            assertThat(remainingItem2.getState()).isEqualTo(OrderItemState.PENDING_PAYMENT);
        }

        @Test
        @DisplayName("ETC 사유와 상세 내용이 히스토리에 저장된다")
        void cancelOrderItems_etcReasonWithDetail() throws Exception {
            // given
            Long memberId = 1L;
            Long orderId = 1L;
            Order order = createOrder(orderId, 1);
            List<Long> cancelItemIds = List.of(1L);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            marketCancelOrderPaymentUseCase.cancelOrderItems(
                    memberId, orderId, cancelItemIds, CancelReasonType.ETC, "개인 사정으로 취소합니다"
            );

            // then
            verify(orderCancelHistoryRepository).saveAll(historyCaptor.capture());
            List<OrderCancelHistory> savedHistories = historyCaptor.getValue();

            OrderCancelHistory history = savedHistories.get(0);
            assertThat(history.getCancelReasonType()).isEqualTo(CancelReasonType.ETC);
            assertThat(history.getCancelReasonDetail()).isEqualTo("개인 사정으로 취소합니다");
        }
    }
}
