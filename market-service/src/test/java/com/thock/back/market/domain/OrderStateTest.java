package com.thock.back.market.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStateTest {

    @Nested
    @DisplayName("isCancellable - 취소/환불 가능 상태 테스트")
    class IsCancellableTest {

        @ParameterizedTest
        @EnumSource(value = OrderState.class, names = {
                "PENDING_PAYMENT", "PAYMENT_COMPLETED", "PREPARING", "SHIPPING", "DELIVERED"
        })
        @DisplayName("구매 확정 전까지는 취소/환불 가능하다")
        void beforeConfirmed_isCancellable(OrderState state) {
            assertThat(state.isCancellable()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = OrderState.class, names = {
                "CONFIRMED", "PARTIALLY_CANCELLED", "CANCELLED",
                "PARTIALLY_REFUNDED", "REFUNDED", "PARTIALLY_SHIPPED"
        })
        @DisplayName("구매 확정 이후 또는 이미 취소/환불된 상태는 취소 불가능하다")
        void afterConfirmedOrCancelled_isNotCancellable(OrderState state) {
            assertThat(state.isCancellable()).isFalse();
        }
    }

    @Nested
    @DisplayName("isConfirmable - 구매 확정 가능 상태 테스트")
    class IsConfirmableTest {

        @Test
        @DisplayName("DELIVERED 상태만 구매 확정 가능하다")
        void onlyDelivered_isConfirmable() {
            assertThat(OrderState.DELIVERED.isConfirmable()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = OrderState.class, names = {
                "PENDING_PAYMENT", "PAYMENT_COMPLETED", "PREPARING",
                "SHIPPING", "CONFIRMED", "PARTIALLY_CANCELLED", "CANCELLED",
                "PARTIALLY_REFUNDED", "REFUNDED", "PARTIALLY_SHIPPED"
        })
        @DisplayName("DELIVERED 외 상태는 구매 확정 불가능하다")
        void otherStates_isNotConfirmable(OrderState state) {
            assertThat(state.isConfirmable()).isFalse();
        }
    }

    @Nested
    @DisplayName("canCompleteRefund - 환불 완료 가능 상태 테스트")
    class CanCompleteRefundTest {

        @Test
        @DisplayName("CANCELLED 상태는 환불 완료 가능하다")
        void cancelled_canCompleteRefund() {
            assertThat(OrderState.CANCELLED.canCompleteRefund()).isTrue();
        }

        @Test
        @DisplayName("PARTIALLY_CANCELLED 상태는 환불 완료 가능하다")
        void partiallyCancelled_canCompleteRefund() {
            assertThat(OrderState.PARTIALLY_CANCELLED.canCompleteRefund()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = OrderState.class, names = {
                "PENDING_PAYMENT", "PAYMENT_COMPLETED", "PREPARING",
                "SHIPPING", "DELIVERED", "CONFIRMED",
                "PARTIALLY_REFUNDED", "REFUNDED", "PARTIALLY_SHIPPED"
        })
        @DisplayName("취소 상태 외에는 환불 완료 불가능하다")
        void otherStates_cannotCompleteRefund(OrderState state) {
            assertThat(state.canCompleteRefund()).isFalse();
        }
    }
}
