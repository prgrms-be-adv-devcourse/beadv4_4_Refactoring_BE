package com.thock.back.product.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.product.ProductServiceApplication;
import com.thock.back.shared.product.event.ProductEvent;
import com.thock.back.shared.product.event.ProductEventType;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = ProductServiceApplication.class,
        properties = {
                "product.outbox.enabled=false",
                "product.inbox.enabled=false",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.listener.auto-startup=false"
        }
)
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopics.PRODUCT_CHANGED}
)
@ActiveProfiles("test")
@DirtiesContext
class ProductOutboxPollerIntegrationTest {

    @Autowired
    private ProductOutboxEventRepository productOutboxEventRepository;

    @Autowired
    @Qualifier("productOutboxKafkaTemplate")
    private KafkaTemplate<String, String> productOutboxKafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Consumer<String, String> consumer;

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
        productOutboxEventRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("poller publishes a pending outbox event and marks it as SENT")
    void pollAndPublish_whenPendingEventExists_publishesAndMarksSent() throws Exception {
        ProductEvent event = ProductEvent.builder()
                .productId(1L)
                .sellerId(101L)
                .name("Keychron Q1")
                .price(230_000L)
                .salePrice(210_000L)
                .stock(15)
                .imageUrl("https://image.example/q1.png")
                .productState("ON_SALE")
                .eventType(ProductEventType.UPDATE)
                .build();

        ProductOutboxEvent pendingEvent = productOutboxEventRepository.save(
                ProductOutboxEvent.create(
                        KafkaTopics.PRODUCT_CHANGED,
                        ProductEvent.class.getName(),
                        "1",
                        objectMapper.writeValueAsString(event)
                )
        );

        ProductOutboxPoller productOutboxPoller =
                new ProductOutboxPoller(productOutboxEventRepository, productOutboxKafkaTemplate);

        consumer = createConsumer();

        transactionTemplate.executeWithoutResult(status -> productOutboxPoller.pollAndPublish());

        ProductOutboxEvent sentEvent = productOutboxEventRepository.findById(pendingEvent.getId()).orElseThrow();
        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.PRODUCT_CHANGED, Duration.ofSeconds(10));
        ProductEvent publishedPayload = objectMapper.readValue(record.value(), ProductEvent.class);
        Header typeHeader = record.headers().lastHeader("__TypeId__");

        assertThat(sentEvent.getStatus()).isEqualTo(ProductOutboxStatus.SENT);
        assertThat(record.key()).isEqualTo("1");
        assertThat(typeHeader).isNotNull();
        assertThat(new String(typeHeader.value(), StandardCharsets.UTF_8))
                .isEqualTo(ProductEvent.class.getName());
        assertThat(publishedPayload.productId()).isEqualTo(1L);
        assertThat(publishedPayload.eventType()).isEqualTo(ProductEventType.UPDATE);
    }

    @Test
    @DisplayName("poller keeps the event pending when Kafka publish fails")
    void pollAndPublish_whenKafkaPublishFails_keepsPendingStatus() throws Exception {
        ProductEvent event = ProductEvent.builder()
                .productId(2L)
                .sellerId(202L)
                .name("Neo65")
                .price(250_000L)
                .salePrice(240_000L)
                .stock(5)
                .imageUrl("https://image.example/neo65.png")
                .productState("ON_SALE")
                .eventType(ProductEventType.UPDATE)
                .build();

        ProductOutboxEvent pendingEvent = productOutboxEventRepository.save(
                ProductOutboxEvent.create(
                        KafkaTopics.PRODUCT_CHANGED,
                        ProductEvent.class.getName(),
                        "2",
                        objectMapper.writeValueAsString(event)
                )
        );

        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> failingKafkaTemplate = mock(KafkaTemplate.class);
        when(failingKafkaTemplate.send(org.mockito.ArgumentMatchers.any(ProducerRecord.class)))
                .thenThrow(new RuntimeException("broker unavailable"));

        ProductOutboxPoller productOutboxPoller =
                new ProductOutboxPoller(productOutboxEventRepository, failingKafkaTemplate);

        transactionTemplate.executeWithoutResult(status -> productOutboxPoller.pollAndPublish());

        ProductOutboxEvent reloadedEvent = productOutboxEventRepository.findById(pendingEvent.getId()).orElseThrow();

        assertThat(reloadedEvent.getStatus()).isEqualTo(ProductOutboxStatus.PENDING);
    }

    private Consumer<String, String> createConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "product-outbox-poller-test-" + UUID.randomUUID(),
                "false",
                embeddedKafkaBroker
        );
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Consumer<String, String> kafkaConsumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer();

        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(kafkaConsumer, KafkaTopics.PRODUCT_CHANGED);
        return kafkaConsumer;
    }
}
