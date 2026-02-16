package com.thock.back.global.outbox.repository;

import com.thock.back.global.outbox.entity.OutboxEvent;
import com.thock.back.global.outbox.entity.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 아웃박스 이벤트 Repository 인터페이스
 * 각 모듈에서 이를 상속받아 구현해야 함
 *
 * 예시:
 * public interface MarketOutboxEventRepository extends OutboxEventRepository<MarketOutboxEvent> {}
 */
@NoRepositoryBean
public interface OutboxEventRepository<T extends OutboxEvent> extends JpaRepository<T, Long> {

    /**
     * PENDING 상태의 이벤트 조회 (생성 순서대로)
     */
    List<T> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    /**
     * PENDING 상태의 이벤트 조회 (제한 개수)
     */
    List<T> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    /**
     * PENDING 상태의 이벤트 조회 + 비관적 락 (SELECT FOR UPDATE)
     * 다른 트랜잭션에서 동시에 같은 이벤트를 가져가지 못하도록 함
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM #{#entityName} e WHERE e.status = :status ORDER BY e.createdAt ASC LIMIT 100")
    List<T> findPendingEventsWithLock(@Param("status") OutboxStatus status);

    /**
     * 특정 시간 이전에 생성된 PUBLISHED 이벤트 삭제 (정리용)
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} e WHERE e.status = 'SENT' AND e.sentAt < :before")
    int deleteSentEventsBefore(@Param("before") LocalDateTime before);

    /**
     * FAILED 상태이고 재시도 횟수가 최대치 미만인 이벤트 조회
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.status = 'FAILED' AND e.retryCount < :maxRetry ORDER BY e.createdAt ASC")
    List<T> findFailedEventsForRetry(@Param("maxRetry") int maxRetry);

    /**
     * 집계 ID로 PENDING 이벤트 존재 여부 확인 (중복 체크용)
     */
    boolean existsByAggregateIdAndEventTypeAndStatus(String aggregateId, String eventType, OutboxStatus status);

    /**
     * 오래된 PROCESSING 이벤트 복구 (인스턴스 장애 대응)
     * processingStartedAt이 특정 시간 이전인 PROCESSING 이벤트를 PENDING으로 리셋
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.status = 'PENDING', e.processingStartedAt = null " +
            "WHERE e.status = 'PROCESSING' AND e.processingStartedAt < :before")
    int recoverStuckProcessingEvents(@Param("before") LocalDateTime before);

    /**
     * PROCESSING 상태의 이벤트 조회 (모니터링/디버깅용)
     */
    List<T> findByStatusOrderByProcessingStartedAtAsc(OutboxStatus status);

}
