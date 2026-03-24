package com.thock.back.market.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemStateTest {

    @Nested
    @DisplayName("isCancellable - 취소 가능 상태 테스트")
    class IsCancellableTest {

        @ParameterizedTest
        @EnumSource(value = OrderItemState.class, names = {
                "PENDING_PAYMENT", "PAYMENT_COMPLETED", "PREPARING", "SHIPPING", "DELIVERED"
        })
        @DisplayName("구매 확정 전까지는 취소 가능하다")
        void beforeConfirmed_isCancellable(OrderItemState state) {
            assertThat(state.isCancellable()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = OrderItemState.class, names = {
                "CONFIRMED", "CANCELLED", "REFUND_REQUESTED", "REFUNDED"
        })
        @DisplayName("구매 확정 이후 또는 이미 취소/환불된 상태는 취소 불가능하다")
        void afterConfirmedOrCancelled_isNotCancellable(OrderItemState state) {
            assertThat(state.isCancellable()).isFalse();
        }
    }

    @Nested
    @DisplayName("isConfirmable - 구매 확정 가능 상태 테스트")
    class IsConfirmableTest {

        @Test
        @DisplayName("DELIVERED 상태만 구매 확정 가능하다")
        void onlyDelivered_isConfirmable() {
            assertThat(OrderItemState.DELIVERED.isConfirmable()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = OrderItemState.class, names = {
                "PENDING_PAYMENT", "PAYMENT_COMPLETED", "PREPARING",
                "SHIPPING", "CONFIRMED", "CANCELLED", "REFUND_REQUESTED", "REFUNDED"
        })
        @DisplayName("DELIVERED 외 상태는 구매 확정 불가능하다")
        void otherStates_isNotConfirmable(OrderItemState state) {
            assertThat(state.isConfirmable()).isFalse();
        }
    }

    @Nested
    @DisplayName("canCompleteRefund - 환불 완료 가능 상태 테스트")
    class CanCompleteRefundTest {

        @Test
        @DisplayName("CANCELLED 상태는 환불 완료 가능하다")
        void cancelled_canCompleteRefund() {
            assertThat(OrderItemState.CANCELLED.canCompleteRefund()).isTrue();
        }

        @Test
        @DisplayName("REFUND_REQUESTED 상태는 환불 완료 가능하다")
        void refundRequested_canCompleteRefund() {
            assertThat(OrderItemState.REFUND_REQUESTED.canCompleteRefund()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = OrderItemState.class, names = {
                "PENDING_PAYMENT", "PAYMENT_COMPLETED", "PREPARING",
                "SHIPPING", "DELIVERED", "CONFIRMED", "REFUNDED"
        })
        @DisplayName("취소/환불요청 외 상태는 환불 완료 불가능하다")
        void otherStates_cannotCompleteRefund(OrderItemState state) {
            assertThat(state.canCompleteRefund()).isFalse();
        }
    }

    @Nested
    @DisplayName("isRefundable - 환불 요청 가능 상태 테스트")
    class IsRefundableTest {

        @Test
        @DisplayName("DELIVERED 상태만 환불 요청 가능하다")
        void delivered_isRefundable() {
            assertThat(OrderItemState.DELIVERED.isRefundable()).isTrue();
        }

        @Test
        @DisplayName("CONFIRMED 상태는 환불 요청 불가능하다 (CS 처리)")
        void confirmed_isNotRefundable() {
            assertThat(OrderItemState.CONFIRMED.isRefundable()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = OrderItemState.class, names = {
                "PENDING_PAYMENT", "PAYMENT_COMPLETED", "PREPARING",
                "SHIPPING", "CANCELLED", "REFUND_REQUESTED", "REFUNDED"
        })
        @DisplayName("DELIVERED 외 상태는 환불 요청 불가능하다")
        void otherStates_isNotRefundable(OrderItemState state) {
            assertThat(state.isRefundable()).isFalse();
        }
    }

    @Nested
    @DisplayName("isActiveState - 정상 진행 상태 테스트")
    class IsActiveStateTest {

        @ParameterizedTest
        @EnumSource(value = OrderItemState.class, names = {
                "PAYMENT_COMPLETED", "PREPARING", "SHIPPING", "DELIVERED"
        })
        @DisplayName("결제완료~배송완료 상태는 정상 진행 상태이다")
        void activeStates_isActiveState(OrderItemState state) {
            assertThat(state.isActiveState()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = OrderItemState.class, names = {
                "PENDING_PAYMENT", "CONFIRMED", "CANCELLED", "REFUND_REQUESTED", "REFUNDED"
        })
        @DisplayName("결제 대기, 구매확정, 취소/환불 상태는 정상 진행 상태가 아니다")
        void nonActiveStates_isNotActiveState(OrderItemState state) {
            assertThat(state.isActiveState()).isFalse();
        }
    }
}
