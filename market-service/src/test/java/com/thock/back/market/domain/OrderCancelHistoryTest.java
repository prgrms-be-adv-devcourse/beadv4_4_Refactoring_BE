package com.thock.back.market.domain;

import com.thock.back.shared.market.domain.CancelReasonType;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCancelHistoryTest {

    private MarketMember buyer;
    private Order order;
    private OrderItem orderItem;

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

        order = new Order(buyer, "12345", "서울시 강남구", "101호");
        setEntityId(order, 1L);

        orderItem = order.addItem(1L, 100L, "테스트상품", "http://image.url", 10000L, 9000L, 1);
        setEntityId(orderItem, 1L);
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
    @DisplayName("팩토리 메서드 테스트")
    class FactoryMethodTest {

        @Test
        @DisplayName("ofUserCancel - 사용자 취소 히스토리 생성")
        void ofUserCancel_createsHistoryWithUserCancelledBy() {
            // when
            OrderCancelHistory history = OrderCancelHistory.ofUserCancel(
                    order, orderItem, CancelReasonType.CHANGE_OF_MIND, "단순 변심입니다"
            );

            // then
            assertThat(history.getOrder()).isEqualTo(order);
            assertThat(history.getOrderItem()).isEqualTo(orderItem);
            assertThat(history.getCancelReasonType()).isEqualTo(CancelReasonType.CHANGE_OF_MIND);
            assertThat(history.getCancelReasonDetail()).isEqualTo("단순 변심입니다");
            assertThat(history.getCancelledBy()).isEqualTo(OrderCancelHistory.CancelledBy.USER);
        }

        @Test
        @DisplayName("ofUserCancel - ETC 사유와 상세 내용 저장")
        void ofUserCancel_etcReasonWithDetail() {
            // when
            OrderCancelHistory history = OrderCancelHistory.ofUserCancel(
                    order, orderItem, CancelReasonType.ETC, "개인 사정으로 인한 취소"
            );

            // then
            assertThat(history.getCancelReasonType()).isEqualTo(CancelReasonType.ETC);
            assertThat(history.getCancelReasonDetail()).isEqualTo("개인 사정으로 인한 취소");
            assertThat(history.getCancelledBy()).isEqualTo(OrderCancelHistory.CancelledBy.USER);
        }

        @Test
        @DisplayName("ofSystemCancel - 시스템 취소 히스토리 생성 (타임아웃)")
        void ofSystemCancel_createsHistoryWithSystemCancelledBy() {
            // when
            OrderCancelHistory history = OrderCancelHistory.ofSystemCancel(
                    order, orderItem, CancelReasonType.PAYMENT_TIMEOUT
            );

            // then
            assertThat(history.getOrder()).isEqualTo(order);
            assertThat(history.getOrderItem()).isEqualTo(orderItem);
            assertThat(history.getCancelReasonType()).isEqualTo(CancelReasonType.PAYMENT_TIMEOUT);
            assertThat(history.getCancelReasonDetail()).isEqualTo("시스템에서 취소(타임 아웃 등)");
            assertThat(history.getCancelledBy()).isEqualTo(OrderCancelHistory.CancelledBy.SYSTEM);
        }

        @Test
        @DisplayName("ofSystemCancel - 사용자 결제 취소 사유")
        void ofSystemCancel_paymentCancelledByUser() {
            // when
            OrderCancelHistory history = OrderCancelHistory.ofSystemCancel(
                    order, orderItem, CancelReasonType.PAYMENT_CANCELLED_BY_USER
            );

            // then
            assertThat(history.getCancelReasonType()).isEqualTo(CancelReasonType.PAYMENT_CANCELLED_BY_USER);
            assertThat(history.getCancelledBy()).isEqualTo(OrderCancelHistory.CancelledBy.SYSTEM);
        }
    }

    @Nested
    @DisplayName("CancelReasonType 시스템 취소 판단 테스트")
    class SystemCancellationTest {

        @Test
        @DisplayName("PAYMENT_TIMEOUT은 시스템 취소이다")
        void paymentTimeout_isSystemCancellation() {
            assertThat(CancelReasonType.PAYMENT_TIMEOUT.isSystemCancellation()).isTrue();
        }

        @Test
        @DisplayName("PAYMENT_CANCELLED_BY_USER는 시스템 취소이다")
        void paymentCancelledByUser_isSystemCancellation() {
            assertThat(CancelReasonType.PAYMENT_CANCELLED_BY_USER.isSystemCancellation()).isTrue();
        }

        @Test
        @DisplayName("CHANGE_OF_MIND는 시스템 취소가 아니다")
        void changeOfMind_isNotSystemCancellation() {
            assertThat(CancelReasonType.CHANGE_OF_MIND.isSystemCancellation()).isFalse();
        }

        @Test
        @DisplayName("PRODUCT_DEFECT는 시스템 취소가 아니다")
        void productDefect_isNotSystemCancellation() {
            assertThat(CancelReasonType.PRODUCT_DEFECT.isSystemCancellation()).isFalse();
        }
    }
}
