package com.thock.back.product.in;

import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.product.app.ProductStockService;
import com.thock.back.product.in.idempotency.ProductInboundEventIdempotencyKeyResolver;
import com.thock.back.product.messaging.inbox.ProductInboxGuard;
import com.thock.back.shared.market.domain.StockEventType;
import com.thock.back.shared.market.dto.StockOrderItemDto;
import com.thock.back.shared.market.event.MarketOrderStockChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 Listener의 InBox 최초/InBox 중복/InBox 비활성화 분기 테스트
 **/

@ExtendWith(MockitoExtension.class)
class ProductKafkaListenerTest {

    private static final String CONSUMER_GROUP = "product-service";

    @Mock
    private ProductStockService productStockService;

    @Mock
    private ObjectProvider<ProductInboxGuard> inboxGuardProvider;

    @Mock
    private ProductInboxGuard inboxGuard;

    @Mock
    private ProductInboundEventIdempotencyKeyResolver keyResolver;

    private ProductKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new ProductKafkaListener(productStockService, inboxGuardProvider, keyResolver);
        ReflectionTestUtils.setField(listener, "consumerGroup", CONSUMER_GROUP);
    }

    @Test
    @DisplayName("handle processes the stock event when inbox claim succeeds")
    void handle_whenClaimed_processesEvent() {
        MarketOrderStockChangedEvent event = stockChangedEvent("ORDER-1", StockEventType.RESERVE);

        when(keyResolver.stockChanged(event)).thenReturn("stock:ORDER-1:RESERVE");
        when(inboxGuardProvider.getIfAvailable()).thenReturn(inboxGuard);
        when(inboxGuard.tryClaim(
                "stock:ORDER-1:RESERVE",
                KafkaTopics.MARKET_ORDER_STOCK_CHANGED,
                CONSUMER_GROUP
        )).thenReturn(true);

        listener.handle(event);

        verify(productStockService).handle(event);
    }

    @Test
    @DisplayName("handle skips the stock event when inbox claim reports a duplicate")
    void handle_whenDuplicate_skipsEvent() {
        MarketOrderStockChangedEvent event = stockChangedEvent("ORDER-2", StockEventType.RELEASE);

        when(keyResolver.stockChanged(event)).thenReturn("stock:ORDER-2:RELEASE");
        when(inboxGuardProvider.getIfAvailable()).thenReturn(inboxGuard);
        when(inboxGuard.tryClaim(
                "stock:ORDER-2:RELEASE",
                KafkaTopics.MARKET_ORDER_STOCK_CHANGED,
                CONSUMER_GROUP
        )).thenReturn(false);

        listener.handle(event);

        verify(productStockService, never()).handle(event);
    }

    @Test
    @DisplayName("handle processes the stock event without inbox when the guard bean is unavailable")
    void handle_whenInboxDisabled_processesEvent() {
        MarketOrderStockChangedEvent event = stockChangedEvent("ORDER-3", StockEventType.COMMIT);

        when(keyResolver.stockChanged(event)).thenReturn("stock:ORDER-3:COMMIT");
        when(inboxGuardProvider.getIfAvailable()).thenReturn(null);

        listener.handle(event);

        verify(productStockService).handle(event);
        verifyNoInteractions(inboxGuard);
    }

    private MarketOrderStockChangedEvent stockChangedEvent(String orderNumber, StockEventType eventType) {
        return new MarketOrderStockChangedEvent(
                orderNumber,
                eventType,
                List.of(
                        new StockOrderItemDto(2L, 1),
                        new StockOrderItemDto(1L, 3)
                )
        );
    }
}
