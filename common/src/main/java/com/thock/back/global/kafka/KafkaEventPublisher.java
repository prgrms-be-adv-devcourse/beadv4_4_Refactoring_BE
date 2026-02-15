package com.thock.back.global.kafka;


import com.thock.back.shared.market.event.*;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import com.thock.back.shared.member.event.MemberModifiedEvent;
import com.thock.back.shared.payment.event.PaymentCompletedEvent;
import com.thock.back.shared.payment.event.PaymentRefundCompletedEvent;
import com.thock.back.shared.settlement.event.SettlementCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(Object event) {
        String topic = resolveTopicName(event);

        if (topic == null) {
            log.debug("No Kafka topic for event: {}", event.getClass().getSimpleName());
            return;
        }

        kafkaTemplate.send(topic, event);
        log.info("Published event to Kafka topic [{}]: {}", topic, event.getClass().getSimpleName());
    }

    private String resolveTopicName(Object event) {
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
        } else if (event instanceof PaymentRefundCompletedEvent) {
            return KafkaTopics.PAYMENT_REFUND_COMPLETED;
        } else if (event instanceof MarketOrderBeforePaymentCanceledEvent) {
            return KafkaTopics.MARKET_ORDER_BEFORE_PAYMENT_REQUEST_CANCELED;
        } else if (event instanceof MarketOrderDeletedEvent) {
            return KafkaTopics.MARKET_ORDER_DELETED;
        } else if (event instanceof MarketOrderSettlementEvent) {
            return KafkaTopics.MARKET_ORDER_SETTLEMENT;
        } else if (event instanceof  PaymentCompletedEvent) {
            return KafkaTopics.PAYMENT_COMPLETED;
        }

        return null;
    }
    /** TODO
     * switch pattern : 자바 21 이상에서 정식 지원, 17 에서도 사용은 가능함 대신 옵션 달아줘야함.
     * 21로 바꾸고 싶은데 ci나 각 모듈들 자바 버전 전부 수정해야하고
     * 팀원들 로컬에서도 temurin 21로 변경해주어야 하기 때문에 나중에 되면 하기로...
     * 가독성이 아쉽긴 함.
     *
     * return switch (event) {
     *             case MemberJoinedEvent e -> KafkaTopics.MEMBER_JOINED;
     *             case MemberModifiedEvent e -> KafkaTopics.MEMBER_MODIFIED;
     *             case MarketOrderPaymentRequestedEvent e -> KafkaTopics.MARKET_ORDER_PAYMENT_REQUESTED;
     *             case MarketOrderPaymentCompletedEvent e -> KafkaTopics.MARKET_ORDER_PAYMENT_COMPLETED;
     *             default -> null;
     *         };
     */
}
