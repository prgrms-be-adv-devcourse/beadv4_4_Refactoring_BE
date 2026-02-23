package com.thock.back.settlement.reconciliation.in;

import com.thock.back.shared.market.event.MarketOrderSettlementEvent;
import com.thock.back.shared.settlement.dto.SettlementOrderItemDto;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MarketOrderSettlementConfirmFlowManualTest {

    private static final String TOPIC = "market.order.settlement";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    @Test
    @DisplayName("수동 실행: PAYMENT_COMPLETED 이후 PURCHASE_CONFIRMED 수신 시 confirmed_at 반영")
    void confirmFlowShouldSetConfirmedAt() throws Exception {
        boolean runKafkaSmoke = Boolean.parseBoolean(System.getProperty("runKafkaSmoke", "false"))
                || Boolean.parseBoolean(System.getenv().getOrDefault("RUN_KAFKA_SMOKE", "false"));
        Assumptions.assumeTrue(runKafkaSmoke);

        String orderNo = "ORD-CONFIRM-SMOKE-" + System.currentTimeMillis();
        long sellerId = 101L;
        long productId = 2001L;
        long amount = 17000L;

        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"
        );

        JsonSerializer<Object> valueSerializer = new JsonSerializer<>();
        valueSerializer.setAddTypeInfo(true);

        DefaultKafkaProducerFactory<String, Object> producerFactory =
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valueSerializer);

        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        try {
            LocalDateTime now = LocalDateTime.now();

            SettlementOrderItemDto paymentCompleted = new SettlementOrderItemDto(
                    orderNo,
                    sellerId,
                    productId,
                    "Confirm Smoke Product",
                    1,
                    amount,
                    amount,
                    "PAYMENT_COMPLETED",
                    Map.of("source", "manual-confirm-flow"),
                    now
            );

            SettlementOrderItemDto purchaseConfirmed = new SettlementOrderItemDto(
                    orderNo,
                    sellerId,
                    productId,
                    "Confirm Smoke Product",
                    1,
                    amount,
                    amount,
                    "PURCHASE_CONFIRMED",
                    Map.of("source", "manual-confirm-flow"),
                    now.plusSeconds(1)
            );

            kafkaTemplate.send(TOPIC, new MarketOrderSettlementEvent(List.of(paymentCompleted))).get(10, TimeUnit.SECONDS);
            kafkaTemplate.send(TOPIC, new MarketOrderSettlementEvent(List.of(purchaseConfirmed))).get(10, TimeUnit.SECONDS);
            kafkaTemplate.flush();

            boolean confirmed = waitUntilConfirmedAtUpdated(orderNo, productId, 20);
            assertThat(confirmed).isTrue();
            assertThat(countPaymentRows(orderNo, productId)).isEqualTo(1);

        } finally {
            producerFactory.destroy();
        }
    }

    private boolean waitUntilConfirmedAtUpdated(String orderNo, long productId, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;

        while (System.currentTimeMillis() < deadline) {
            if (isPaymentRowConfirmed(orderNo, productId)) {
                return true;
            }
            Thread.sleep(500L);
        }

        return false;
    }

    private boolean isPaymentRowConfirmed(String orderNo, long productId) throws Exception {
        String url = "jdbc:mysql://localhost:3307/thock_settlement_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
        String sql = """
                SELECT confirmed_at
                FROM sales_log
                WHERE order_no = ?
                  AND product_id = ?
                  AND transaction_type = 'PAYMENT'
                ORDER BY id DESC
                LIMIT 1
                """;

        try (Connection connection = DriverManager.getConnection(url, "root", "root");
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, orderNo);
            ps.setLong(2, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return rs.getTimestamp("confirmed_at") != null;
            }
        }
    }

    private int countPaymentRows(String orderNo, long productId) throws Exception {
        String url = "jdbc:mysql://localhost:3307/thock_settlement_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
        String sql = """
                SELECT COUNT(*)
                FROM sales_log
                WHERE order_no = ?
                  AND product_id = ?
                  AND transaction_type = 'PAYMENT'
                """;

        try (Connection connection = DriverManager.getConnection(url, "root", "root");
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, orderNo);
            ps.setLong(2, productId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
