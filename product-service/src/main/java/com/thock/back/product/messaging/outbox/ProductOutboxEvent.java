package com.thock.back.product.messaging.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_outbox_event", indexes = {
        @Index(name = "idx_product_outbox_status_created", columnList = "status, createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, length = 200)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String eventKey;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductOutboxStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ProductOutboxEvent create(String topic, String eventType, String eventKey, String payload) {
        ProductOutboxEvent event = new ProductOutboxEvent();
        event.topic = topic;
        event.eventType = eventType;
        event.eventKey = eventKey;
        event.payload = payload;
        event.status = ProductOutboxStatus.PENDING;
        event.createdAt = LocalDateTime.now();
        return event;
    }

    public void markAsSent() {
        this.status = ProductOutboxStatus.SENT;
    }
}
