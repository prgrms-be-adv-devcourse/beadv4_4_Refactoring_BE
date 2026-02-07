package com.thock.back.market.domain;

import com.thock.back.global.exception.CustomException;
import com.thock.back.shared.market.domain.CancelReasonType;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 도메인 단위 테스트
 * 도메인 객체 직접 생성 / 비즈니스 규칙만 검증
 */
class OrderTest {

    private MarketMember buyer;
    private Order order;

    @BeforeEach
    void setUp() throws Exception{
        // MarketPolicy의 static 필드 설정 (Spring Context 없이 테스트하기 위해)
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

        order = new Order(buyer, "12345", "서울시 강남구", "101호");
        setEntityId(order, 1L);
    }

    private OrderItem addItemToOrder(Order order, Long itemId) throws Exception {
        OrderItem item = order.addItem(
                1L,           // sellerId
                100L,         // productId
                "테스트상품",   // productName
                "http://image.url", // productImageUrl
                10000L,       // price
                9000L,        // salePrice
                1             // quantity
        );

        // Reflection으로 ID 설정 (JPA 자동생성 대신)
        setEntityId(item, itemId);
        return item;
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
    @DisplayName("cancelItems 부분 취소 테스트")
    class CancelItemsTest {

        @Test
        @DisplayName("결제 전 상태에서 부분 취소가 성공한다")
        void cancelItems_beforePayment_success() throws Exception {
            // given
            OrderItem item = addItemToOrder(order, 1L);
            assertThat(item.getState()).isEqualTo(OrderItemState.PENDING_PAYMENT);

            // when
            order.cancelItems(List.of(1L), CancelReasonType.CHANGE_OF_MIND, null);

            // then
            assertThat(item.getState()).isEqualTo(OrderItemState.CANCELLED);
        }

        @Test
        @DisplayName("존재하지 않는 아이템 취소 시 예외가 발생한다")
        void cancelItems_notFound_throwsException() throws Exception {
            // given
            addItemToOrder(order, 1L);

            // when & then
            assertThatThrownBy(() -> order.cancelItems(List.of(999L), CancelReasonType.CHANGE_OF_MIND, null))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("취소 불가능한 상태(SHIPPING)에서 취소 시 예외가 발생한다")
        void cancelItems_shippingState_throwsException() throws Exception {
            // given
            OrderItem item = addItemToOrder(order, 1L);

            // 상태를 SHIPPING으로 변경 (Reflection 사용)
            Field stateField = OrderItem.class.getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(item, OrderItemState.SHIPPING);

            // when & then
            assertThatThrownBy(() -> order.cancelItems(List.of(1L), CancelReasonType.CHANGE_OF_MIND, null))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("updateStateFromItems 주문 상태 업데이트 테스트")
    class UpdateStateFromItemsTest {

        @Test
        @DisplayName("일부 아이템 취소 시 주문 상태가 PARTIALLY_CANCELLED가 된다")
        void partialCancel_stateBecomesPartiallyCancelled() throws Exception {
            // given
            OrderItem item1 = addItemToOrder(order, 1L);
            OrderItem item2 = addItemToOrder(order, 2L);

            // item1만 취소
            order.cancelItems(List.of(1L), CancelReasonType.CHANGE_OF_MIND, null);

            // then
            assertThat(item1.getState()).isEqualTo(OrderItemState.CANCELLED);
            assertThat(item2.getState()).isEqualTo(OrderItemState.PENDING_PAYMENT);
            assertThat(order.getState()).isEqualTo(OrderState.PARTIALLY_CANCELLED);
        }

        @Test
        @DisplayName("모든 아이템 취소 시 주문 상태가 CANCELLED가 된다")
        void allCancel_stateBecomesAllCancelled() throws Exception {
            // given
            addItemToOrder(order, 1L);
            addItemToOrder(order, 2L);

            // when - 모든 아이템 한 번에 취소
            order.cancelItems(List.of(1L, 2L), CancelReasonType.CHANGE_OF_MIND, null);

            // then
            assertThat(order.getState()).isEqualTo(OrderState.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Order 생성 테스트")
    class OrderCreationTest {

        @Test
        @DisplayName("주문 생성 시 초기 상태는 PENDING_PAYMENT이다")
        void createOrder_initialState_isPendingPayment() {
            assertThat(order.getState()).isEqualTo(OrderState.PENDING_PAYMENT);
        }

        @Test
        @DisplayName("주문 생성 시 주문번호가 생성된다")
        void createOrder_orderNumberGenerated() {
            assertThat(order.getOrderNumber()).startsWith("ORDER-");
            assertThat(order.getOrderNumber()).hasSize(27); // ORDER-yyyyMMdd-xxxxxxxxxxxx (27자)
        }

        @Test
        @DisplayName("buyer가 null이면 예외가 발생한다")
        void createOrder_nullBuyer_throwsException() {
            assertThatThrownBy(() -> new Order(null, "12345", "주소", "상세"))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("addItem 테스트")
    class AddItemTest {

        @Test
        @DisplayName("아이템 추가 시 총 금액이 계산된다")
        void addItem_calculatesTotalPrice() throws Exception {
            // given & when
            order.addItem(1L, 100L, "상품1", "url", 10000L, 9000L, 2);

            // then
            assertThat(order.getTotalPrice()).isEqualTo(20000L);      // 10000 * 2
            assertThat(order.getTotalSalePrice()).isEqualTo(18000L);  // 9000 * 2
            assertThat(order.getTotalDiscountAmount()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("여러 아이템 추가 시 총 금액이 누적된다")
        void addMultipleItems_accumulatesTotalPrice() throws Exception {
            // when
            order.addItem(1L, 100L, "상품1", "url", 10000L, 9000L, 1);
            order.addItem(2L, 200L, "상품2", "url", 20000L, 18000L, 1);

            // then
            assertThat(order.getTotalPrice()).isEqualTo(30000L);
            assertThat(order.getTotalSalePrice()).isEqualTo(27000L);
        }
    }
}
