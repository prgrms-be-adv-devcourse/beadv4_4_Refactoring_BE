package com.thock.back.global.outbox;

import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.global.outbox.entity.OutboxEvent;
import com.thock.back.shared.market.event.MarketMemberCreatedEvent;
import com.thock.back.shared.market.event.MarketOrderBeforePaymentCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderDeletedEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentCompletedEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestedEvent;
import com.thock.back.shared.market.event.MarketOrderSettlementEvent;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import com.thock.back.shared.member.event.MemberModifiedEvent;
import com.thock.back.shared.payment.event.PaymentCompletedEvent;
import com.thock.back.shared.payment.event.PaymentRefundCompletedEvent;
import com.thock.back.shared.settlement.event.SettlementCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Outbox 패턴을 사용하는 이벤트 발행자 추상 클래스
 *
 * 기존 EventPublisher를 대체하여 사용
 * - Local 이벤트: ApplicationEventPublisher로 즉시 발행 (같은 서비스 내 리스너용)
 * - Kafka 이벤트: Outbox 테이블에 저장 (스케줄러가 폴링하여 발행)
 *
 * 각 모듈에서 이를 상속받아 구현:
 * @Component
 * public class MarketEventPublisher extends OutboxEventPublisher<MarketOutboxEvent> {
 *     // ...
 * }
 */
@Slf4j
public abstract class OutboxEventPublisher<T extends OutboxEvent> {

    protected final ApplicationEventPublisher applicationEventPublisher;
    protected final OutboxEventSaver<T> outboxEventSaver;

    protected OutboxEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                                   OutboxEventSaver<T> outboxEventSaver) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.outboxEventSaver = outboxEventSaver;
    }

    /**
     * 이벤트 발행
     * - Local: 즉시 발행
     * - Kafka: Outbox 테이블에 저장 (트랜잭션과 함께 커밋)
     */
    public void publish(Object event) {
        // Local event for same-service listeners
        applicationEventPublisher.publishEvent(event);

        // Outbox에 저장 (Kafka 발행은 스케줄러가 처리)
        String topic = resolveTopicName(event);
        if (topic != null) {
            EventInfo eventInfo = extractEventInfo(event);
            outboxEventSaver.save(
                    eventInfo.aggregateType(),
                    eventInfo.aggregateId(),
                    topic,
                    event
            );
            log.debug("Outbox에 이벤트 저장: type={}, topic={}", event.getClass().getSimpleName(), topic);
        }
    }

    /**
     * 이벤트에서 집계 정보 추출
     */
    protected EventInfo extractEventInfo(Object event) {
        if (event instanceof MemberJoinedEvent e) {
            return new EventInfo("Member", e.member().id().toString());
        } else if (event instanceof MemberModifiedEvent e) {
            return new EventInfo("Member", e.member().id().toString());
        } else if (event instanceof MarketOrderPaymentRequestedEvent e) {
            return new EventInfo("Order", e.order().id().toString());
        } else if (event instanceof MarketOrderPaymentCompletedEvent e) {
            return new EventInfo("Order", e.order().id().toString());
        } else if (event instanceof MarketOrderPaymentRequestCanceledEvent e) {
            return new EventInfo("Order", e.dto().orderId());
        } else if (event instanceof MarketMemberCreatedEvent e) {
            return new EventInfo("MarketMember", e.member().id().toString());
        } else if (event instanceof PaymentCompletedEvent e) {
            return new EventInfo("Payment", e.payment().id().toString());
        } else if (event instanceof PaymentRefundCompletedEvent e) {
            return new EventInfo("Payment", e.dto().orderId());
        } else if (event instanceof SettlementCompletedEvent e) {
            return new EventInfo("Settlement", e.memberID().toString());
        } else if (event instanceof MarketOrderBeforePaymentCanceledEvent e) {
            return new EventInfo("Order", e.dto().orderId());
        } else if (event instanceof MarketOrderDeletedEvent e) {
            return new EventInfo("Order", e.dto().orderNumber());
        } else if (event instanceof MarketOrderSettlementEvent e) {
            return new EventInfo("Order", e.items().isEmpty() ? "unknown" : e.items().get(0).orderNo());
        }

        return new EventInfo("Unknown", "unknown");
    }

    /**
     * 토픽 이름 결정
     */
    protected String resolveTopicName(Object event) {
        if (event instanceof MemberJoinedEvent) {
            return KafkaTopics.MEMBER_JOINED;
        } else if (event instanceof MemberModifiedEvent) {
            return KafkaTopics.MEMBER_MODIFIED;
        } else if (event instanceof MarketOrderPaymentRequestedEvent) {
            return KafkaTopics.MARKET_ORDER_PAYMENT_REQUESTED;
        } else if (event instanceof MarketOrderPaymentCompletedEvent) {
            return KafkaTopics.MARKET_ORDER_PAYMENT_COMPLETED;
        } else if (event instanceof MarketOrderPaymentRequestCanceledEvent) {
            return KafkaTopics.MARKET_ORDER_PAYMENT_REQUEST_CANCELED;
        } else if (event instanceof SettlementCompletedEvent) {
            return KafkaTopics.SETTLEMENT_COMPLETED;
        } else if (event instanceof PaymentCompletedEvent) {
            return KafkaTopics.PAYMENT_COMPLETED;
        } else if (event instanceof PaymentRefundCompletedEvent) {
            return KafkaTopics.PAYMENT_REFUND_COMPLETED;
        } else if (event instanceof MarketOrderBeforePaymentCanceledEvent) {
            return KafkaTopics.MARKET_ORDER_BEFORE_PAYMENT_REQUEST_CANCELED;
        } else if (event instanceof MarketOrderDeletedEvent) {
            return KafkaTopics.MARKET_ORDER_DELETED;
        } else if (event instanceof MarketOrderSettlementEvent) {
            return KafkaTopics.MARKET_ORDER_SETTLEMENT;
        }

        return null;
    }

    /**
     * 이벤트 정보를 담는 record
     */
    protected record EventInfo(String aggregateType, String aggregateId) {}
}
