package com.thock.back.global.outbox.repository;

import com.thock.back.global.outbox.entity.OutboxEvent;
import com.thock.back.global.outbox.entity.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * PENDING 조회 (생성순(ASC) + 개수 제한은 Pageable로)
     * 로컬/단일 인스턴스에서 폴링
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(
            @Param("status") OutboxStatus status,
            Pageable pageable
    );

    /**
     * PENDING 조회 + 비관적 락
     * 운영에서 스케일아웃 (ex. market-serice 2개 이상) 예정이면 락 버전으로 가는게 안전
     * TODO 비관적 락 VS Shed 분산 락
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEventsWithLock(
            @Param("status") OutboxStatus status,
            Pageable pageable
    );

    /**
     * SENT 정리
     * Outbox는 계속 쌓이면 테이블이 무한 증가함
     * 주기적으로 삭제해주는 작업
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'SENT' AND o.sentAt < :before")
    int deleteSentEventsBefore(@Param("before") LocalDateTime before);

    /**
     * FAILED - 재시도 가능한 실패 이벤트 조회용
     */
    @Query("""
                SELECT o
                FROM OutboxEvent o
                WHERE o.status = 'FAILED'
                  AND o.retryCount < :maxRetry
                ORDER BY o.createdAt ASC
            """)
    List<OutboxEvent> findFailedEventsForRetry(@Param("maxRetry") int maxRetry);

    // 중복 체크
    boolean existsByAggregateIdAndEventTypeAndStatus(
            String aggregateId,
            String eventType,
            OutboxStatus status
    );

    // PROCESSING stuck 복구
    @Modifying
    @Query("""
            UPDATE OutboxEvent o
            SET o.status = 'PENDING',
                o.processingStartedAt = null
            WHERE o.status = 'PROCESSING'
              AND o.processingStartedAt < :before
            """)
    int recoverStuckProcessingEvents(@Param("before") LocalDateTime before);

    // 모니터링용
    List<OutboxEvent> findByStatusOrderByProcessingStartedAtAsc(OutboxStatus status);

}
