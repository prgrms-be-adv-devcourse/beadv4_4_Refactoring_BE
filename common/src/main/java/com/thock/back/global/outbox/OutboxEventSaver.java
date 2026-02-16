package com.thock.back.global.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thock.back.global.outbox.entity.OutboxEvent;
import com.thock.back.global.outbox.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * 이벤트를 Outbox 테이블에 저장하는 추상 클래스
 * 각 모듈에서 이를 상속받아 구현해야 함
 *
 * 예시:
 * @Component
 * public class MarketOutboxEventSaver extends OutboxEventSaver<MarketOutboxEvent> {
 *     public MarketOutboxEventSaver(MarketOutboxEventRepository repository, ObjectMapper objectMapper) {
 *         super(repository, objectMapper);
 *     }
 *
 *     @Override
 *     protected MarketOutboxEvent createOutboxEvent(...) {
 *         return new MarketOutboxEvent(...);
 *     }
 * }
 */
@Slf4j
public abstract class OutboxEventSaver<T extends OutboxEvent> {

    protected final OutboxEventRepository<T> outboxRepository;
    protected final ObjectMapper objectMapper;

    protected OutboxEventSaver(OutboxEventRepository<T> outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 이벤트를 Outbox 테이블에 저장
     * 비즈니스 트랜잭션과 함께 커밋됨
     *
     * @param aggregateType 집계 루트 타입 (예: "Order")
     * @param aggregateId 집계 루트 ID (예: orderId.toString())
     * @param topic Kafka 토픽명
     * @param event 이벤트 객체
     */
    public void save(String aggregateType, String aggregateId, String topic, Object event) {
        try {
            String eventType = event.getClass().getSimpleName();
            String payload = objectMapper.writeValueAsString(event);

            T outboxEvent = createOutboxEvent(aggregateType, aggregateId, eventType, topic, payload);
            outboxRepository.save(outboxEvent);

            log.debug("Outbox 이벤트 저장: type={}, aggregateId={}, topic={}",
                    eventType, aggregateId, topic);

        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    /**
     * 각 모듈의 OutboxEvent 구현체를 생성
     * 서브클래스에서 구현해야 함
     */
    protected abstract T createOutboxEvent(String aggregateType, String aggregateId,
                                           String eventType, String topic, String payload);
}
