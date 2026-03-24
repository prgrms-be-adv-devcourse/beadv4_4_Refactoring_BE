package com.thock.back.global.eventPublisher;

import com.thock.back.global.kafka.KafkaEventPublisher;
import com.thock.back.global.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaEventPublisher kafkaEventPublisher;
    // outbox.enabled=true일 때만 빈이 존재하므로 optional 주입
    private final ObjectProvider<OutboxEventPublisher> outboxEventPublisherProvider;

    @Value("${outbox.enabled:false}")
    private boolean outboxEnabled;

    public void publish(Object event) {
        // 1) 같은 서비스 내부 리스너용 로컬 이벤트는 항상 발행
        applicationEventPublisher.publishEvent(event);

        // 2) 서비스 간 이벤트는 설정에 따라 Outbox 또는 즉시 Kafka 발행
        if (outboxEnabled) {
            OutboxEventPublisher outbox = outboxEventPublisherProvider.getIfAvailable();

            if (outbox != null) {
                // 트랜잭션 내 Outbox 저장 (Poller가 Kafka로 비동기 발행)
                outbox.saveToOutbox(event);
            } else {
                // 안전장치: Outbox 빈이 없으면 즉시 Kafka로 fallback
                kafkaEventPublisher.publish(event);
            }
        } else {
            // Outbox 비활성화 시 기존 방식(즉시 Kafka 발행)
            kafkaEventPublisher.publish(event);
        }
    }
}
