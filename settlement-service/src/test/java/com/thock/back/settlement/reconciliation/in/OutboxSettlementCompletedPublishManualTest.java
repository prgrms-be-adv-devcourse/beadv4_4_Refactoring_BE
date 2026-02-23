package com.thock.back.settlement.reconciliation.in;

import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.global.outbox.entity.OutboxEvent;
import com.thock.back.global.outbox.repository.OutboxEventRepository;
import com.thock.back.shared.settlement.event.SettlementCompletedEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "outbox.enabled=true",
        "outbox.poller.enabled=false"
})
@ActiveProfiles("local-test")
class OutboxSettlementCompletedPublishManualTest {

    @MockBean
    private com.thock.back.settlement.settlement.app.service.SettlementBatchLauncher settlementBatchLauncher;

    @MockBean
    private JobRepository jobRepository;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    @Transactional
    @DisplayName("수동 실행: SettlementCompletedEvent 발행 시 outbox_event 적재")
    void saveSettlementCompletedEventToOutbox() {
        boolean runOutboxSmoke = Boolean.parseBoolean(System.getProperty("runOutboxSmoke", "false"))
                || Boolean.parseBoolean(System.getenv().getOrDefault("RUN_OUTBOX_SMOKE", "false"));
        Assumptions.assumeTrue(runOutboxSmoke);

        long memberId = 999001L;
        long amount = 12345L;

        eventPublisher.publish(new SettlementCompletedEvent(memberId, amount));
        outboxEventRepository.flush();

        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertThat(events)
                .anyMatch(event -> KafkaTopics.SETTLEMENT_COMPLETED.equals(event.getTopic())
                        && String.valueOf(memberId).equals(event.getAggregateId())
                        && event.getPayload().contains("\"memberID\":" + memberId)
                        && event.getPayload().contains("\"amount\":" + amount));
    }
}
