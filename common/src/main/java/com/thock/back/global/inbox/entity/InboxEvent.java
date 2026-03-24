package com.thock.back.global.inbox.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbox_event", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_inbox_idempotency_key_consumer_group",
                columnNames = {"idempotencyKey", "consumerGroup"}
        )
}, indexes = {
        @Index(name = "idx_inbox_topic_consumer_group", columnList = "topic, consumerGroup"),
        @Index(name = "idx_inbox_created_at", columnList = "createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, length = 100)
    private String consumerGroup;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static InboxEvent create(String idempotencyKey, String topic, String consumerGroup) {
        InboxEvent event = new InboxEvent();
        event.idempotencyKey = idempotencyKey;
        event.topic = topic;
        event.consumerGroup = consumerGroup;
        event.createdAt = LocalDateTime.now();
        return event;
    }
}
