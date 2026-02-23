package com.thock.back.settlement.reconciliation.in;

import com.thock.back.shared.market.event.MarketOrderSettlementEvent;
import com.thock.back.shared.settlement.dto.SettlementOrderItemDto;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class MarketOrderSettlementEventPublishManualTest {

    private static final String TOPIC = "market.order.settlement";
    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";

    @Test
    @DisplayName("수동 실행: market.order.settlement 토픽으로 가짜 이벤트 발행")
    void publishFakeSettlementEvent() throws Exception {
        // 기본 테스트 실행 시에는 skip. 수동 확인 시 runKafkaSmoke 시스템 프로퍼티나 RUN_KAFKA_SMOKE 환경변수를 사용.
        boolean runKafkaSmoke = Boolean.parseBoolean(System.getProperty("runKafkaSmoke", "false"))
                || Boolean.parseBoolean(System.getenv().getOrDefault("RUN_KAFKA_SMOKE", "false"));
        Assumptions.assumeTrue(runKafkaSmoke);

        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getProperty("kafkaBootstrapServers", DEFAULT_BOOTSTRAP_SERVERS),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"
        );

        JsonSerializer<Object> valueSerializer = new JsonSerializer<>();
        valueSerializer.setAddTypeInfo(true);

        DefaultKafkaProducerFactory<String, Object> producerFactory =
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valueSerializer);

        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        try {
            SettlementOrderItemDto item = new SettlementOrderItemDto(
                    "ORD-SMOKE-001",
                    101L,
                    2001L,
                    "Smoke Product",
                    1,
                    15000L,
                    15000L,
                    "PAYMENT_COMPLETED",
                    Map.of("source", "manual-test"),
                    LocalDateTime.now()
            );

            MarketOrderSettlementEvent event = new MarketOrderSettlementEvent(List.of(item));

            kafkaTemplate.send(TOPIC, event).get(10, TimeUnit.SECONDS);
            kafkaTemplate.flush();
        } finally {
            producerFactory.destroy();
        }
    }
}
