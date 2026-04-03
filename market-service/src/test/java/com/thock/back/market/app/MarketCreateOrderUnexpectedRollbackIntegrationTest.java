package com.thock.back.market.app;

import com.thock.back.market.domain.OrderState;
import com.thock.back.market.in.dto.req.OrderCreateRequest;
import com.thock.back.market.in.dto.res.OrderCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarketCreateOrderUnexpectedRollbackIntegrationTest {

    @InjectMocks
    private MarketFacade marketFacade;

    @Mock
    private MarketSyncMemberUseCase marketSyncMemberUseCase;

    @Mock
    private MarketCreateCartUseCase marketCreateCartUseCase;

    @Mock
    private MarketCreateOrderUseCase marketCreateOrderUseCase;

    @Mock
    private MarketCompleteOrderPaymentUseCase marketCompleteOrderPaymentUseCase;

    @Mock
    private MarketCancelOrderPaymentUseCase marketCancelOrderPaymentUseCase;

    @Mock
    private MarketCompleteRefundUseCase marketCompleteRefundUseCase;

    @Mock
    private MarketConfirmOrderUseCase marketConfirmOrderUseCase;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Test
    @DisplayName("동시 요청으로 unique 충돌이 발생해도 UnexpectedRollbackException 없이 기존 주문을 반환한다")
    void createOrder_uniqueConflict_doesNotThrowUnexpectedRollbackException_andReturnsExistingOrder() {
        Long memberId = 1L;
        String idempotencyKey = "idem-key-1";
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(1L),
                "12345",
                "서울시 강남구",
                "101호"
        );

        OrderCreateResponse existingOrder = new OrderCreateResponse(
                1L,
                "ORDER-20260403-TEST",
                OrderState.PENDING_PAYMENT,
                List.of(),
                50_000L,
                40_000L,
                10_000L,
                40_000L,
                "12345",
                "서울시 강남구",
                "101호",
                LocalDateTime.now()
        );

        given(marketCreateOrderUseCase.findExistingOrderByIdempotencyKey(memberId, idempotencyKey))
                .willReturn(null, existingOrder);
        given(marketCreateOrderUseCase.createOrder(memberId, request, idempotencyKey))
                .willThrow(new DataIntegrityViolationException("unique constraint violation"));

        // 과거에는 같은 트랜잭션 내부에서 예외를 복구하려다 UnexpectedRollbackException이 발생했지만,
        // 현재는 Facade가 비트랜잭션으로 동작하므로 기존 주문 응답으로 정상 복구되어야 한다.
        OrderCreateResponse response = marketFacade.createOrder(memberId, request, idempotencyKey);

        assertThat(response).isEqualTo(existingOrder);
        verify(marketCreateOrderUseCase).createOrder(memberId, request, idempotencyKey);
        verify(marketCreateOrderUseCase, times(2))
                .findExistingOrderByIdempotencyKey(memberId, idempotencyKey);
    }
}
