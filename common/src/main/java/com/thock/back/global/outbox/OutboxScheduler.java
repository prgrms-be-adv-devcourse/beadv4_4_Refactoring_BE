package com.thock.back.global.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thock.back.global.outbox.entity.OutboxEvent;
import com.thock.back.global.outbox.entity.OutboxStatus;
import com.thock.back.global.outbox.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Outbox 테이블을 폴링하여 Kafka로 발행하는 스케줄러 추상 클래스
 * 각 모듈에서 이를 상속받아 @Scheduled 메서드를 정의해야 함
 *
 * 예시:
 * @Component
 * public class MarketOutboxScheduler extends OutboxScheduler<MarketOutboxEvent> {
 *
 *     @Scheduled (fixedDelay = 1000) // 1초마다 폴링
 *     public void poll() {
 *         publishPendingEvents();
 *     }
 *
 *     @Scheduled (cron = "0 0 * * * *")  // 매시간 정리
 *     public void cleanup() {
 *         cleanupPublishedEvents(Duration.ofDays(7));
 *     }
 * }
 */
@Slf4j
public abstract class OutboxScheduler<T extends OutboxEvent> {

    protected final OutboxEventRepository<T> outboxRepository;
    protected final KafkaTemplate<String, Object> kafkaTemplate;
    protected final ObjectMapper objectMapper;

    private static final int MAX_RETRY_COUNT = 5;
    private static final int KAFKA_SEND_TIMEOUT_SECONDS = 10;
    private static final int PROCESSING_TIMEOUT_MINUTES = 5;  // PROCESSING 상태 타임아웃

    protected OutboxScheduler(OutboxEventRepository<T> outboxRepository,
                              KafkaTemplate<String, Object> kafkaTemplate,
                              ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * PENDING 상태의 이벤트를 Kafka로 발행
     * 각 모듈에서 @Scheduled로 호출
     *
     * 1.PENDING 이벤트 조회
     * 2. PROCESSING으로 상태 변경 (claim)
     * 3. Kafka 발행
     * 4. SENT 또는 FAILED로 상태 변경
     */
    @Transactional
    public void publishPendingEvents() {
        List<T> pendingEvents = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Outbox 폴링: {} 개의 PENDING 이벤트 발견", pendingEvents.size());

        for (T event : pendingEvents) {
            // 먼저 PROCESSING으로 상태 변경 (claim)

            event.markAsProcessing();
            publishEvent(event);
        }
    }

    /**
     * FAILED 상태의 이벤트 재시도
     */
    @Transactional
    public void retryFailedEvents() {
        List<T> failedEvents = outboxRepository.findFailedEventsForRetry(MAX_RETRY_COUNT);

        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("Outbox 재시도: {} 개의 FAILED 이벤트", failedEvents.size());

        for (T event : failedEvents) {
            event.resetForRetry();
            publishEvent(event);
        }
    }

    /**
     * 오래된 SENT 이벤트 정리
     */
    @Transactional
    public void cleanupSentEvents(java.time.Duration retention) {
        LocalDateTime before = LocalDateTime.now().minus(retention);
        int deleted = outboxRepository.deleteSentEventsBefore(before);

        if (deleted > 0) {
            log.info("Outbox 정리: {} 개의 오래된 이벤트 삭제", deleted);
        }
    }

    /**
     * 오래된 PROCESSING 이벤트 복구 (인스턴스 장애 대응)
     * PROCESSING 상태로 오래 남아있는 이벤트를 PENDING으로 리셋
     * 각 모듈에서 @Scheduled로 주기적으로 호출 권장
     */
    @Transactional
    public void recoverStuckEvents(){
        LocalDateTime before = LocalDateTime.now().minusMinutes(PROCESSING_TIMEOUT_MINUTES);
        int recovered = outboxRepository.recoverStuckProcessingEvents(before);

        if (recovered > 0) {
            log.warn("OutBox 복구: {} 개의 PROCESSING 이벤트를 PENDING으로 리셋 (타임아웃: {}분)", recovered, PROCESSING_TIMEOUT_MINUTES);
        }

    }

    /**
     * 단일 이벤트 발행
     */
    private void publishEvent(T outboxEvent) {
        try {
            Object event = deserializeEvent(outboxEvent);

            // 동기적으로 Kafka 발행 (결과 확인)
            kafkaTemplate.send(outboxEvent.getTopic(), event)
                    .get(KAFKA_SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            outboxEvent.markAsSent();

            log.info("Outbox 이벤트 발행 성공: id={}, traceId={}, type={}, topic={}",
                    outboxEvent.getId(), outboxEvent.getTraceId(),
                    outboxEvent.getEventType(), outboxEvent.getTopic());

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            String errorMessage = e.getMessage();
            outboxEvent.markAsFailed(errorMessage);

            log.error("Outbox 이벤트 발행 실패: id={}, traceId={}, type={}, retry={}, error={}",
                    outboxEvent.getId(), outboxEvent.getTraceId(),
                    outboxEvent.getEventType(), outboxEvent.getRetryCount(), errorMessage);

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            outboxEvent.markAsFailed(e.getMessage());
            log.error("Outbox 이벤트 처리 중 예외: id={}, traceId={}",
                    outboxEvent.getId(), outboxEvent.getTraceId(), e);
        }
    }

    /**
     * JSON payload를 이벤트 객체로 역직렬화
     * 서브클래스에서 오버라이드 가능
     */
    protected Object deserializeEvent(T outboxEvent) {
        try {
            Class<?> eventClass = resolveEventClass(outboxEvent.getEventType());
            return objectMapper.readValue(outboxEvent.getPayload(), eventClass);
        } catch (Exception e) {
            log.error("이벤트 역직렬화 실패: id={}, traceId={}, type={}",
                    outboxEvent.getId(), outboxEvent.getTraceId(), outboxEvent.getEventType(), e);
            throw new RuntimeException("이벤트 역직렬화 실패", e);
        }
    }

    /**
     * 이벤트 타입 문자열로부터 Class 객체를 반환
     * 각 모듈에서 구현해야 함
     */
    protected abstract Class<?> resolveEventClass(String eventType);
}
