package com.thock.back.product.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "product.outbox", name = "enabled", havingValue = "true")
public class ProductOutboxPoller {

    private final ProductOutboxEventRepository productOutboxEventRepository;
    private final @Qualifier("productOutboxKafkaTemplate") KafkaTemplate<String, String> productOutboxKafkaTemplate;

    @Scheduled(fixedDelayString = "${product.outbox.poller.interval-ms:3000}")
    @Transactional
    public void pollAndPublish() {
        List<ProductOutboxEvent> events = productOutboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(ProductOutboxStatus.PENDING);

        for (ProductOutboxEvent event : events) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(event.getTopic(), event.getEventKey(), event.getPayload());
                record.headers().add("__TypeId__", event.getEventType().getBytes(StandardCharsets.UTF_8));

                productOutboxKafkaTemplate.send(record).get();
                event.markAsSent();
            } catch (Exception e) {
                log.error("Failed to publish product outbox event. id={}, error={}", event.getId(), e.getMessage());
            }
        }
    }
}
