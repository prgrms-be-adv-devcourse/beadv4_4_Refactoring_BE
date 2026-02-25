package com.thock.back.market.monitoring;

import com.thock.back.global.outbox.entity.OutboxStatus;
import com.thock.back.global.outbox.repository.OutboxEventRepository;
import com.thock.back.market.domain.OrderState;
import com.thock.back.market.out.repository.OrderRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "market.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MarketMetricsCollector {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final MeterRegistry meterRegistry;
    private final AdminClient adminClient;
    private final String consumerGroupId;

    private final AtomicLong orderTotal = new AtomicLong(0);
    private final AtomicLong outboxTotal = new AtomicLong(0);
    private final AtomicLong outboxFailedRatioPercent = new AtomicLong(0);
    private final AtomicLong outboxBacklogRatioPercent = new AtomicLong(0);
    private final AtomicLong kafkaLagTotal = new AtomicLong(0);

    private final Map<OrderState, AtomicLong> orderStateCount = new EnumMap<>(OrderState.class);
    private final Map<OutboxStatus, AtomicLong> outboxStatusCount = new EnumMap<>(OutboxStatus.class);
    private final Map<OutboxStatus, AtomicLong> outboxStatusRatioPercent = new EnumMap<>(OutboxStatus.class);
    private final Map<String, AtomicLong> kafkaLagByTopic = new HashMap<>();
    private final Set<String> registeredLagTopics = ConcurrentHashMap.newKeySet();

    public MarketMetricsCollector(
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            MeterRegistry meterRegistry,
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${market.metrics.consumer-group-id:market-service}") String consumerGroupId
    ) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.meterRegistry = meterRegistry;
        this.consumerGroupId = consumerGroupId;

        Map<String, Object> adminConfig = new HashMap<>();
        adminConfig.put("bootstrap.servers", bootstrapServers);
        this.adminClient = AdminClient.create(adminConfig);

        Gauge.builder("market_order_total_count", orderTotal, AtomicLong::get)
                .description("Total number of market orders")
                .register(meterRegistry);
        Gauge.builder("market_outbox_total_count", outboxTotal, AtomicLong::get)
                .description("Total number of outbox events")
                .register(meterRegistry);
        Gauge.builder("market_outbox_failed_ratio_percent", outboxFailedRatioPercent, AtomicLong::get)
                .description("FAILED outbox ratio percent")
                .register(meterRegistry);
        Gauge.builder("market_outbox_backlog_ratio_percent", outboxBacklogRatioPercent, AtomicLong::get)
                .description("Backlog outbox ratio percent (PENDING+PROCESSING+FAILED)")
                .register(meterRegistry);
        Gauge.builder("market_kafka_consumer_lag_total", kafkaLagTotal, AtomicLong::get)
                .description("Total consumer lag for market-service group")
                .register(meterRegistry);

        for (OrderState state : OrderState.values()) {
            AtomicLong gaugeValue = new AtomicLong(0);
            orderStateCount.put(state, gaugeValue);
            Gauge.builder("market_order_state_count", gaugeValue, AtomicLong::get)
                    .description("Order count by state")
                    .tag("state", state.name())
                    .register(meterRegistry);
        }

        for (OutboxStatus status : OutboxStatus.values()) {
            AtomicLong countGauge = new AtomicLong(0);
            AtomicLong ratioGauge = new AtomicLong(0);
            outboxStatusCount.put(status, countGauge);
            outboxStatusRatioPercent.put(status, ratioGauge);

            Gauge.builder("market_outbox_status_count", countGauge, AtomicLong::get)
                    .description("Outbox event count by status")
                    .tag("status", status.name())
                    .register(meterRegistry);
            Gauge.builder("market_outbox_status_ratio_percent", ratioGauge, AtomicLong::get)
                    .description("Outbox event ratio percent by status")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }
    }

    @Scheduled(fixedDelayString = "${market.metrics.collect-interval-ms:10000}")
    public void collect() {
        collectOrderAndOutboxMetrics();
        collectKafkaLagMetrics();
    }

    private void collectOrderAndOutboxMetrics() {
        long totalOrders = 0;
        for (OrderState state : OrderState.values()) {
            long count = orderRepository.countByState(state);
            orderStateCount.get(state).set(count);
            totalOrders += count;
        }
        orderTotal.set(totalOrders);

        Map<OutboxStatus, Long> counts = new EnumMap<>(OutboxStatus.class);
        long totalOutbox = 0;
        for (OutboxStatus status : OutboxStatus.values()) {
            long count = outboxEventRepository.countByStatus(status);
            counts.put(status, count);
            outboxStatusCount.get(status).set(count);
            totalOutbox += count;
        }
        outboxTotal.set(totalOutbox);

        long backlog = counts.getOrDefault(OutboxStatus.PENDING, 0L)
                + counts.getOrDefault(OutboxStatus.PROCESSING, 0L)
                + counts.getOrDefault(OutboxStatus.FAILED, 0L);
        long failed = counts.getOrDefault(OutboxStatus.FAILED, 0L);

        for (OutboxStatus status : OutboxStatus.values()) {
            long count = counts.getOrDefault(status, 0L);
            long ratio = totalOutbox == 0 ? 0 : (count * 100) / totalOutbox;
            outboxStatusRatioPercent.get(status).set(ratio);
        }

        outboxBacklogRatioPercent.set(totalOutbox == 0 ? 0 : (backlog * 100) / totalOutbox);
        outboxFailedRatioPercent.set(totalOutbox == 0 ? 0 : (failed * 100) / totalOutbox);
    }

    private void collectKafkaLagMetrics() {
        try {
            ListConsumerGroupOffsetsResult groupOffsetsResult = adminClient.listConsumerGroupOffsets(consumerGroupId);
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> committed =
                    groupOffsetsResult.partitionsToOffsetAndMetadata().get();

            if (committed.isEmpty()) {
                kafkaLagTotal.set(0);
                return;
            }

            Map<TopicPartition, OffsetSpec> latestReq = new HashMap<>();
            for (TopicPartition tp : committed.keySet()) {
                latestReq.put(tp, OffsetSpec.latest());
            }

            ListOffsetsResult latestOffsetsResult = adminClient.listOffsets(latestReq);

            long totalLag = 0L;
            Map<String, Long> lagByTopic = new HashMap<>();

            for (Map.Entry<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> entry : committed.entrySet()) {
                TopicPartition tp = entry.getKey();
                long committedOffset = entry.getValue().offset();
                long latestOffset = latestOffsetsResult.partitionResult(tp).get().offset();
                long lag = Math.max(latestOffset - committedOffset, 0L);

                totalLag += lag;
                lagByTopic.merge(tp.topic(), lag, Long::sum);
            }

            kafkaLagTotal.set(totalLag);

            for (Map.Entry<String, Long> entry : lagByTopic.entrySet()) {
                String topic = entry.getKey();
                AtomicLong gauge = kafkaLagByTopic.computeIfAbsent(topic, t -> new AtomicLong(0));
                gauge.set(entry.getValue());
                registerTopicLagGaugeIfNeeded(topic, gauge);
            }

            for (Map.Entry<String, AtomicLong> entry : kafkaLagByTopic.entrySet()) {
                if (!lagByTopic.containsKey(entry.getKey())) {
                    entry.getValue().set(0);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to collect kafka lag metrics: {}", e.getMessage());
        }
    }

    private void registerTopicLagGaugeIfNeeded(String topic, AtomicLong value) {
        if (!registeredLagTopics.add(topic)) {
            return;
        }
        Gauge.builder("market_kafka_consumer_lag_topic", value, AtomicLong::get)
                .description("Consumer lag by topic for market-service group")
                .tag("topic", topic)
                .register(meterRegistry);
    }

    @PreDestroy
    public void close() {
        adminClient.close();
    }
}
