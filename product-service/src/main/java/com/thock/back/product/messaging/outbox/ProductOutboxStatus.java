package com.thock.back.product.messaging.outbox;

public enum ProductOutboxStatus {
    PENDING, // 아직 Kafka로 안 보냄
    SENT, // 발행 성공
}
