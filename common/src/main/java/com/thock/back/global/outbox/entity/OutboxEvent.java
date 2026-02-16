package com.thock.back.global.outbox.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 아웃박스 패턴을 위한 기본 엔티티
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public abstract class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 집계 루트 타입 == 이벤트 발생 도메인 이름 (예: Order, Payment)
     */
    @Column(nullable = false, length = 100)
    private String aggregateType;

    /**
     * 집계 루트 ID == 도메인 키 (예: orderId, paymentId)
     */
    @Column(nullable = false, length = 100)
    private String aggregateId;

    /**
     * 이벤트 타입 (예: MarketOrderPaymentRequestedEvent)
     */
    @Column(nullable = false, length = 200)
    private String eventType;


    /**
     * Kafka 토픽명
     */
    @Column(nullable = false, length = 200)
    private String topic;

    /**
     * 이벤트 페이로드 == 이벤트 상세 데이터 (JSON 직렬화, ex. {"memberId": 300, "amount": 5000})
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * 이벤트 발행 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    /**
     * 재시도 횟수 - 재시도 정책 적용
     */
    @Column(nullable = false)
    private Integer retryCount;

    /**
     * 실패 에러 메시지
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 이벤트 생성 시간
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * MQ 이벤트 발행 시간
     */
    private LocalDateTime sentAt;

    /**
     * 처리 시작 시간 -PROCESSING 상태 진입 시점
     */
    private LocalDateTime processingStartedAt;

    /**
     * 분산 추적 ID - 이벤트 추적용 고유 식별자
     * Zipkin, Jaeger 등 분산 추적 시스템과 연동 가능
     */
    @Column(nullable = false, length = 36)
    private String traceId;

    /**
     * TODO
     * 현재 구조는 1초마다 폴링, 목적 : Exponential Backoff 구현
     * 1차 실패 → 1초 후 재시도
     * 2차 실패 → 4초 후 재시도
     * 3차 실패 → 16초 후 재시도
     * 있으면 좋은데 추후에 더 생각해보자
     *
     * 이벤트 재발행 예약 시간 - 배치에서 scheduledAt <= now AND status = 'PENDING' 조회
     * private LocalDateTime scheduled_at;
     */


    public OutboxEvent(String aggregateType, String aggregateId, String eventType,
                       String topic, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.traceId = UUID.randomUUID().toString();
    }

    /**
     * 처리 시작 (이벤트 Claim)
     * 다른 인스턴스/프로세스의 중복 처리 방지
     */
    public void markAsProcessing() {
        this.status = OutboxStatus.PROCESSING;
        this.processingStartedAt = LocalDateTime.now();
    }

    /**
     * 발행 성공 처리
     */
    public void markAsSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 발행 실패 처리
     */
    public void markAsFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.retryCount++;
        this.errorMessage = error;
    }

    /**
     * 재시도를 위해 PENDING으로 리셋
     */
    public void resetForRetry() {
        this.status = OutboxStatus.PENDING;
    }

    /**
     * 최대 재시도 횟수 초과 여부
     */
    public boolean isMaxRetryExceeded(int maxRetry) {
        return this.retryCount >= maxRetry;
    }
}
