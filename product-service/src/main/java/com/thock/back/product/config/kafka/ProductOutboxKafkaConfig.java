package com.thock.back.product.config.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 OutBox 전용 Kafka producer를 추가합니다. 현재 ProductKafkaConfig.java 는 KafkaTemplate<String, Object> 기준입니다. OutBox poller는 DB에 저장된 JSON 문자열을 그대로 보내는 구조라서 KafkaTemplate<String, String>가 더 맞습니다.
 **/

@Configuration
public class ProductOutboxKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean("productOutboxProducerFactory")
    public ProducerFactory<String, String> productOutboxProducerFactory() {
        Map<String, Object>  props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer());
    }

    @Bean("productOutboxKafkaTemplate")
    public KafkaTemplate<String, String> productOutboxKafkaTemplate() {
        return new KafkaTemplate<>(productOutboxProducerFactory());
    }
}
