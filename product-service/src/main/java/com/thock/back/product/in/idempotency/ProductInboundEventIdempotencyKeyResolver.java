package com.thock.back.product.in.idempotency;

import com.thock.back.shared.market.event.MarketOrderStockChangedEvent;
import org.springframework.stereotype.Component;

@Component
public class ProductInboundEventIdempotencyKeyResolver {

    // 이벤트의 고유성을 보장하기 위해 orderNumber, eventType, 그리고 items의 조합을 키로 사용
    public String stockChanged(MarketOrderStockChangedEvent event) {
        String itemsPart = event.items().stream()
                .sorted((a, b) -> Long.compare(a.productId(), b.productId()))
                .map(i -> i.productId() + "-" + i.quantity())
                .reduce((a, b) -> a + "," + b)
                .orElse("empty");

        return event.orderNumber() + ":" + event.eventType().name() + ":" + itemsPart;
    }
}
