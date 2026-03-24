package com.thock.back.product.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.product.messaging.publisher.ProductEventPublisher;
import com.thock.back.shared.product.event.ProductEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductOutboxEventPublisher implements ProductEventPublisher {

    private final ProductOutboxEventRepository productOutboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(ProductEvent event) {
        try {
            ProductOutboxEvent outboxEvent = ProductOutboxEvent.create(
                    KafkaTopics.PRODUCT_CHANGED,
                    ProductEvent.class.getName(),
                    String.valueOf(event.productId()),
                    objectMapper.writeValueAsString(event)
            );

            productOutboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ProductEvent to JSON", e);
        }
    }
}
