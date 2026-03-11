package com.thock.back.product.in;

import com.thock.back.global.inbox.InboxGuard;
import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.product.app.ProductStockService;
import com.thock.back.product.in.idempotency.ProductInboundEventIdempotencyKeyResolver;
import com.thock.back.shared.market.event.MarketOrderStockChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductKafkaListener {

    private static final String PRODUCT_CONSUMER_GROUP = "product-service";

    private final ProductStockService productStockService;
    private final ObjectProvider<InboxGuard> inboxGuardProvider;
    private final ProductInboundEventIdempotencyKeyResolver keyResolver;

    // Kafka 토픽에서 재고 변경 이벤트를 수신하여 처리하는 메서드 -> DLQ 설정이 적용된 KafkaListener로, 이벤트 처리 중 예외가 발생하면 자동으로 DLQ로 이동
    @KafkaListener(
            topics = KafkaTopics.MARKET_ORDER_STOCK_CHANGED,
            groupId = PRODUCT_CONSUMER_GROUP,
            containerFactory = "productKafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(MarketOrderStockChangedEvent event) {
        String key = keyResolver.stockChanged(event);
        if (!shouldProcess(KafkaTopics.MARKET_ORDER_STOCK_CHANGED, key)) {
            log.info("Duplicate stock event ignored. orderNumber={}, eventType={}", event.orderNumber(), event.eventType());
            return;
        }

        productStockService.handle(event);
        log.info("Stock event processed. orderNumber={}, eventType={}, itemCount={}",
                event.orderNumber(), event.eventType(), event.items().size());
    }

    // 이벤트의 중복 처리를 방지하기 위해 InboxGuard를 사용하여 이벤트 처리 여부를 결정하는 메서드
    private boolean shouldProcess(String topic, String idempotencyKey) {
        InboxGuard inboxGuard = inboxGuardProvider.getIfAvailable();
        if (inboxGuard == null) {
            return true;
        }
        return inboxGuard.tryClaim(idempotencyKey, topic, PRODUCT_CONSUMER_GROUP);
    }
}
