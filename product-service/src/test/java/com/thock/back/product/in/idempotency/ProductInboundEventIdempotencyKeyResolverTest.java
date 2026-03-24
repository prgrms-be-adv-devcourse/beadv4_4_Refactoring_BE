package com.thock.back.product.in.idempotency;

import com.thock.back.shared.market.domain.StockEventType;
import com.thock.back.shared.market.dto.StockOrderItemDto;
import com.thock.back.shared.market.event.MarketOrderStockChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 Idempotency key 안정성 테스트
 **/

class ProductInboundEventIdempotencyKeyResolverTest {

    private final ProductInboundEventIdempotencyKeyResolver resolver =
            new ProductInboundEventIdempotencyKeyResolver();

    @Test
    @DisplayName("stockChanged returns the same key when item order differs")
    void stockChanged_whenOnlyItemOrderDiffers_returnsSameKey() {
        MarketOrderStockChangedEvent first = new MarketOrderStockChangedEvent(
                "ORDER-1",
                StockEventType.RESERVE,
                List.of(
                        new StockOrderItemDto(3L, 1),
                        new StockOrderItemDto(1L, 2)
                )
        );
        MarketOrderStockChangedEvent second = new MarketOrderStockChangedEvent(
                "ORDER-1",
                StockEventType.RESERVE,
                List.of(
                        new StockOrderItemDto(1L, 2),
                        new StockOrderItemDto(3L, 1)
                )
        );

        String firstKey = resolver.stockChanged(first);
        String secondKey = resolver.stockChanged(second);

        assertThat(firstKey).isEqualTo(secondKey);
    }

    @Test
    @DisplayName("stockChanged returns a different key when quantity changes")
    void stockChanged_whenQuantityDiffers_returnsDifferentKey() {
        MarketOrderStockChangedEvent baseEvent = new MarketOrderStockChangedEvent(
                "ORDER-1",
                StockEventType.RESERVE,
                List.of(new StockOrderItemDto(1L, 2))
        );
        MarketOrderStockChangedEvent changedEvent = new MarketOrderStockChangedEvent(
                "ORDER-1",
                StockEventType.RESERVE,
                List.of(new StockOrderItemDto(1L, 3))
        );

        String baseKey = resolver.stockChanged(baseEvent);
        String changedKey = resolver.stockChanged(changedEvent);

        assertThat(baseKey).isNotEqualTo(changedKey);
    }
}
