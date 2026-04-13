package com.thock.back.market.monitoring.sql;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MarketRequestSqlMetricsRecorder {

    private final MeterRegistry meterRegistry;

    @Value("${market.metrics.n-plus-one-select-threshold:15}")
    private int nPlusOneSelectThreshold;

    public void record(MarketRequestSqlMetricsContext.Snapshot snapshot, int status) {
        if (snapshot == null) {
            return;
        }

        Tags tags = Tags.of(
                "method", snapshot.method(),
                "uri", snapshot.uri(),
                "status", String.valueOf(status)
        );

        DistributionSummary.builder("market_http_db_queries")
                .description("Total SQL statements executed per HTTP request")
                .baseUnit("queries")
                .serviceLevelObjectives(1, 3, 5, 10, 20, 50, 100, 200)
                .tags(tags)
                .register(meterRegistry)
                .record(snapshot.totalQueries());

        DistributionSummary.builder("market_http_db_select_queries")
                .description("Total SELECT statements executed per HTTP request")
                .baseUnit("queries")
                .serviceLevelObjectives(1, 3, 5, 10, 20, 50, 100, 200)
                .tags(tags)
                .register(meterRegistry)
                .record(snapshot.selectQueries());

        Timer.builder("market_http_db_time")
                .description("Total DB execution time per HTTP request")
                .tags(tags)
                .publishPercentileHistogram()
                .serviceLevelObjectives(
                        Duration.ofMillis(5),
                        Duration.ofMillis(10),
                        Duration.ofMillis(25),
                        Duration.ofMillis(50),
                        Duration.ofMillis(100),
                        Duration.ofMillis(250),
                        Duration.ofMillis(500),
                        Duration.ofSeconds(1)
                )
                .register(meterRegistry)
                .record(snapshot.totalQueryTimeMs(), TimeUnit.MILLISECONDS);

        if (snapshot.selectQueries() >= nPlusOneSelectThreshold) {
            Counter.builder("market_http_db_n_plus_one_suspected_total")
                    .description("Requests suspected of N+1 due to high SELECT count")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
    }
}
