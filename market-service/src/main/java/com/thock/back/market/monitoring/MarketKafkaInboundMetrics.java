package com.thock.back.market.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class MarketKafkaInboundMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> receivedCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> processedCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> duplicateCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> failedCounters = new ConcurrentHashMap<>();

    public MarketKafkaInboundMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordReceived(String topic) {
        counter(receivedCounters, "market_kafka_inbound_received_total", topic).increment();
    }

    public void recordProcessed(String topic) {
        counter(processedCounters, "market_kafka_inbound_processed_total", topic).increment();
    }

    public void recordDuplicate(String topic) {
        counter(duplicateCounters, "market_kafka_inbound_duplicate_total", topic).increment();
    }

    public void recordFailed(String topic) {
        counter(failedCounters, "market_kafka_inbound_failed_total", topic).increment();
    }

    private Counter counter(ConcurrentMap<String, Counter> map, String metricName, String topic) {
        return map.computeIfAbsent(topic, key ->
                Counter.builder(metricName)
                        .description("Market inbound kafka metric")
                        .tag("topic", key)
                        .register(meterRegistry)
        );
    }
}
