package com.thock.back.product.messaging.publisher;

import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.shared.product.event.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProductEventPublisher implements ProductEventPublisher {

    private final @Qualifier("productKafkaTemplate") KafkaTemplate<String, Object> productKafkaTemplate;

    @Override
    public void publish(ProductEvent event) {
        String key = String.valueOf(event.productId());
        productKafkaTemplate.send(KafkaTopics.PRODUCT_CHANGED, key, event);
        log.info("Published product event. productId={}, eventType={}", event.productId(), event.eventType());
    }
}
