package com.thock.back.global.outbox;

import com.thock.back.global.outbox.entity.OutboxEvent;
import com.thock.back.global.outbox.entity.OutboxStatus;
import com.thock.back.global.outbox.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnExpression("${outbox.enabled:false} and ${outbox.poller.enabled:true}")
/**
 * @ConditionalOnProperty(name = "outbox.poller.enabled", havingValue = "true", matchIfMissing = true)
 * outbox.poller.enabled가 yml에 없어도 (matchIfMissing = true)
 * poller 빈이 생성 됨 / 즉, outbox.enabled=false여도 Poller는 살아있을 수 있음
 *
 * 문제 1. Poller가 주기적으로 DB 조회/로그 남김 (불필요한 부하)
 * 문제 2. outbox를 안 쓰는 서비스에서도 스케줄러가 돈다는 의미
 *
 * havingValue="true" - 해당 프로퍼티 값이 true일 때만 빈 생성
 * matchIfMissing=true - 프로퍼티가 없어도 생성
 */
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> outboxKafkaTemplate;

    public OutboxPoller(OutboxEventRepository outboxEventRepository,
                        KafkaTemplate<String, String> outboxKafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxKafkaTemplate = outboxKafkaTemplate;
    }

    @Value("${outbox.poller.batch-size:100}")
    private int batchSize;

    @Value("${outbox.poller.max-retry:5}")
    private int maxRetry;

    @Value("${outbox.poller.send-timeout-seconds:10}")
    private int sendTimeoutSeconds;

    @Value("${outbox.poller.processing-timeout-minutes:5}")
    private int processingTimeoutMinutes;

    @Scheduled(fixedDelayString = "${outbox.poller.interval-ms:5000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            event.markAsProcessing();

            /**
             * 파티션이 null이면 Kafka 기본 파티셔너가 결정
             * key가 있으므로 보통 key(aggregateId) 해시 기반으로 같은 key는 같은 파티션에 가서 순서 보장이 된다
             */
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    event.getTopic(), null, event.getAggregateId(), event.getPayload()
            );
            record.headers().add("__TypeId__", event.getEventType().getBytes(StandardCharsets.UTF_8));
            outboxKafkaTemplate.send(record).get(sendTimeoutSeconds, TimeUnit.SECONDS);

            event.markAsSent();
            log.debug("Published outbox event: id={}, topic={}", event.getId(), event.getTopic());

        } catch (Exception e) {
            log.error("Failed to publish outbox event: id={}, error={}", event.getId(), e.getMessage());
            event.markAsFailed(e.getMessage());
        }
    }

    /**
     * FAILED 상태의 이벤트 재시도
     */
    @Scheduled(fixedDelayString = "${outbox.poller.retry-interval-ms:10000}")
    @Transactional
    public void retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxEventRepository.findFailedEventsForRetry(maxRetry);

        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("Outbox 재시도: {} 개의 FAILED 이벤트", failedEvents.size());

        for (OutboxEvent event : failedEvents) {
            event.resetForRetry();
            processEvent(event);
        }

    }

    /**
     * 오래된 SENT 이벤트 정리, 매일 새벽 3시
     */
    @Scheduled(cron = "${outbox.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupOldSentEvents() {
        LocalDateTime before = LocalDateTime.now().minusDays(7);
        int deleted = outboxEventRepository.deleteSentEventsBefore(before);

        if (deleted > 0) {
            log.info("Outbox 정리: {} 개의 오래된 이벤트 삭제", deleted);
        }
    }

    /**
     * 오래된 PROCESSING 이벤트 복구 (인스턴스 장애 대응)
     * PROCESSING 상태로 오래 남아있는 이벤트를 PENDING으로 리셋
     */
    @Scheduled(fixedDelayString = "${outbox.poller.recover-interval-ms:30000}")
    @Transactional
    public void recoverStuckProcessingEvents(){
        LocalDateTime before = LocalDateTime.now().minusMinutes(processingTimeoutMinutes);
        int recovered = outboxEventRepository.recoverStuckProcessingEvents(before);

        if (recovered > 0) {
            log.warn("OutBox 복구: {} 개의 PROCESSING 이벤트를 PENDING으로 리셋", recovered);
        }

    }
}
