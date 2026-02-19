package com.thock.back.global.kafka;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Configuration
@EnableKafka
/**
 * 강사님 피드백 - 충분하다. 설정 분리하기 보다는 지금처럼 하는게 편할거다. 배포하면서 이슈 생길때마다 설정 추가할 부분 있으면 추가해줄 것
 *
 * 공통 설정만 두고 각 모듈에 따로 작성을 해야하는지? yml이나 common에 abstract두고 각 모듈에서 extends 하는 식으로
 * Concurrency - 컨슈머 스레드 수 = 동시에 처리하는 파티션 수 ⭐️ 파티션 수에 따라
 * AckMode - 언제 이벤트를 처리 완료로 볼 것인지 (RECORD, BATCH, MANUAL, TIME / COUNT) ⭐️ 중요도에 따라
 * MAX-POLL-RECORDS - 한 번에 가져올 레코드 수 제한 / 기본값 500 ⭐️ 트래픽에 따라
 * DLQ(Dead Letter Queue) - 이벤트가 실패했을 때 몇 번 재시도하고 그래도 안되면 어떻게 처리할지 -> 이건 yml로는 한계가 있음. ⭐️ 이벤트 종류에 따라
 *
 * ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, -> 기본값은 latest
 *
 */
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private ObjectMapper kafkaObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // kafka 3.0 이상 버전 부터 Producer 멱등성 자동 보장해주지만 버전이 바뀔 수도 있고, 명시해주는게 확실하다 생각함
        // 멱등성 보장 (재시도 해도 같은 메시지를 브로커에 두 번 append 하는 걸 방지(producer id, sequence number)
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

        // RETRIES_CONFIG를 크게 설정했으면 꼭 추가해주어야 하는 설정, retries는 보조 옵션, 실패 판단은 시간 기준으로 해야 함
        // Kafka 공식 문서 권장: retries는 충분히 크게 두고, 실제 실패 여부는 delivery.timeout.ms 초과로 판단
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000); // 2분
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30_000); // 30초

        /**
         *  JsonSerializer<Object> + setAddTypeInfo(true)
         *  - 일반 이벤트 발행 경로에서 Object(여러 이벤트 클래스)를 그대로 보냄
         *  - 소비자에서 어떤 클래스인지 알아야 역직렬화 가능
         *  - type header(__TypeId__)를 붙여서 클래스 정보를 전달
         */
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(kafkaObjectMapper());
        jsonSerializer.setAddTypeInfo(true);

        return new DefaultKafkaProducerFactory<>(
                configProps,
                new StringSerializer(),
                jsonSerializer
        );
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Outbox용 String Producer
     * Outbox에서 payload는 이미 JSON 문자열로 저장되어 있으므로
     * StringSerializer를 사용해야 함 (JsonSerializer 사용 시 이중 직렬화 문제 발생)
     * 타입 정보도 eventType으로 관리 가능
     */
    @Bean
    public ProducerFactory<String, String> outboxProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        /**
         * Outbox용 신뢰성 설정
         * 즉시 발행 RETRIES=Integer.MAX_VALUE vs outbox RETRIES=3 이유
         * 재시도 정책을 즉시 발행은 producer에서, outbox는 poller + DB에서로 책임 분리
         *
         * 즉시 발행
         * - 요청 트랜잭션/흐름 안에서 바로 보내는 모델이라 producer 측 재시도를 크게 잡아 "당장 전달 성공" 확률을 높이려는 의도
         * outbox
         * - 실패해도 DB outbox에 남아서 poller가 다시 재시도 함
         * - producer 내부 재시도를 무한으로 하면 poller 스레드가 오래 묶일 수 있음
         * - 그래서 재시도 횟수를 짧게 두고, 큰 재시도는 outbox 레벨에서 처리하도록 함
         */
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");                 // 모든 복제본 확인
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);    // 멱등성 보장
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);                  // 재시도 횟수
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate() {
        return new KafkaTemplate<>(outboxProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // 기본값 latest 이지만 명시

        // ErrorHandlingDeserializer : 역직렬화 에러로 컨슈머 죽는거 방지
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // 보안 : 신뢰할 패키지만 지정
        // 역직렬화(JSON -> Java) 시 인증된 패키지에서만 클래스 생성 가능하도록 함
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.thock.back.shared.*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // 오프셋 관리 : 수동 커밋
        // TODO 모듈에 각각 설정 다르게 작성해주어야 할듯 -> 공통에서 하기로 함
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // 성능 : 한 번에 가져올 레코드 수 제한 / 기본값 500
        // TODO 모듈에 각각 설정 다르게 작성해주어야 할듯 -> 공통에서 하기로 함
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // 수동 커밋 모드 : RECORD - 레코드 1건 처리 될 때마다 커밋, 성능이 상대적으로 낮음.
        // TODO 모듈에 각각 설정 다르게 작성해주어야 할듯
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }
}
