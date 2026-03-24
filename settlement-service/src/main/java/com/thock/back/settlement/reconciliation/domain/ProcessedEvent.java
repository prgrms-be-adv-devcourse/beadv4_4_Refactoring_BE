package com.thock.back.settlement.reconciliation.domain;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "processed_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_processed_event_key", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_processed_event_order_no", columnList = "order_no"),
                @Index(name = "idx_processed_event_processed_at", columnList = "processed_at")
        }
)
public class ProcessedEvent {

    @Id
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "order_no", nullable = false, length = 255)
    private String orderNo;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Builder
    public ProcessedEvent(String idempotencyKey, String source, String eventType, String orderNo) {
        this.idempotencyKey = idempotencyKey;
        this.source = source;
        this.eventType = eventType;
        this.orderNo = orderNo;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = TsidCreator.getTsid().toLong();
        }
        if (this.processedAt == null) {
            this.processedAt = LocalDateTime.now();
        }
    }
}
