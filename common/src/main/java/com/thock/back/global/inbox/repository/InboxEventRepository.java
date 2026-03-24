package com.thock.back.global.inbox.repository;

import com.thock.back.global.inbox.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {

    @Modifying
    @Query(
            value = """
                    INSERT IGNORE INTO inbox_event
                    (idempotency_key, topic, consumer_group, created_at)
                    VALUES (:idempotencyKey, :topic, :consumerGroup, NOW(6))
                    """,
            nativeQuery = true
    )
    int claimIfAbsent(@Param("idempotencyKey") String idempotencyKey,
                      @Param("topic") String topic,
                      @Param("consumerGroup") String consumerGroup);
}
