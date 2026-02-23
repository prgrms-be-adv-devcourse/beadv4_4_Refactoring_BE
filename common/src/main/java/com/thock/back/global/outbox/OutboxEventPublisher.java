package com.thock.back.global.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.global.outbox.entity.OutboxEvent;
import com.thock.back.global.outbox.repository.OutboxEventRepository;
import com.thock.back.shared.market.event.MarketMemberCreatedEvent;
import com.thock.back.shared.market.event.MarketOrderBeforePaymentCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderDeletedEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentCompletedEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestedEvent;
import com.thock.back.shared.market.event.MarketOrderSettlementEvent;
import com.thock.back.shared.market.event.MarketOrderStockChangedEvent;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import com.thock.back.shared.member.event.MemberModifiedEvent;
import com.thock.back.shared.payment.event.PaymentCompletedEvent;
import com.thock.back.shared.payment.event.PaymentRefundCompletedEvent;
import com.thock.back.shared.settlement.event.SettlementCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "outbox", name = "enabled", havingValue = "true")
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveToOutbox(Object event) {
        OutboxEventMetadata metadata = resolveMetadata(event);

        if (metadata == null || metadata.topic() == null) {
            log.debug("No outbox metadata for event: {}", event.getClass().getSimpleName());
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.create(
                    metadata.aggregateType(),
                    metadata.aggregateId(),
                    event.getClass().getName(), // getName -> FQN(Fully Quality Name) : 역직렬화/라우팅에 안정적
                    metadata.topic(),
                    payload
            );

            outboxEventRepository.save(outboxEvent);
            log.debug("Saved event to outbox: type={}, aggregateId={}",
                    metadata.aggregateType(), metadata.aggregateId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event to JSON", e);
        }
    }

    /**
     * 이벤트에서 집계 정보 추출
     */
    private OutboxEventMetadata resolveMetadata(Object event) {
        if (event instanceof MemberJoinedEvent e) {
            return new OutboxEventMetadata("Member", String.valueOf(e.member().id()), KafkaTopics.MEMBER_JOINED);
        } else if (event instanceof MemberModifiedEvent e) {
            return new OutboxEventMetadata("Member", String.valueOf(e.member().id()), KafkaTopics.MEMBER_MODIFIED);
        } else if (event instanceof MarketOrderPaymentRequestedEvent e) {
            return new OutboxEventMetadata("Order", String.valueOf(e.order().id()), KafkaTopics.MARKET_ORDER_PAYMENT_REQUESTED);
        } else if (event instanceof MarketOrderPaymentCompletedEvent e) {
            return new OutboxEventMetadata("Order", String.valueOf(e.order().id()), KafkaTopics.MARKET_ORDER_PAYMENT_COMPLETED);
        } else if (event instanceof MarketOrderPaymentRequestCanceledEvent e) {
            return new OutboxEventMetadata("Order", e.dto().orderId(), KafkaTopics.MARKET_ORDER_PAYMENT_REQUEST_CANCELED);
        } else if (event instanceof PaymentCompletedEvent e) {
            return new OutboxEventMetadata("Payment", String.valueOf(e.payment().id()), KafkaTopics.PAYMENT_COMPLETED);
        } else if (event instanceof PaymentRefundCompletedEvent e) {
            return new OutboxEventMetadata("Payment", e.dto().orderId(), KafkaTopics.PAYMENT_REFUND_COMPLETED);
        } else if (event instanceof SettlementCompletedEvent e) {
            return new OutboxEventMetadata("Settlement", String.valueOf(e.memberID()), KafkaTopics.SETTLEMENT_COMPLETED);
        } else if (event instanceof MarketOrderBeforePaymentCanceledEvent e) {
            return new OutboxEventMetadata("Order", e.dto().orderId(), KafkaTopics.MARKET_ORDER_BEFORE_PAYMENT_REQUEST_CANCELED);
        } else if (event instanceof MarketOrderDeletedEvent e) {
            return new OutboxEventMetadata("Order", e.dto().orderNumber(), KafkaTopics.MARKET_ORDER_DELETED);
        } else if (event instanceof MarketOrderSettlementEvent e) {
            return new OutboxEventMetadata(
                    "Order",
                    e.items().isEmpty() ? "unknown" : e.items().get(0).orderNo(),
                    KafkaTopics.MARKET_ORDER_SETTLEMENT
            );
        } else if (event instanceof MarketOrderStockChangedEvent e) {
            return new OutboxEventMetadata("Order", e.orderNumber(), KafkaTopics.MARKET_ORDER_STOCK_CHANGED);
        }

        return new OutboxEventMetadata("Unknown", "unknown", null);
    }



    /**
     * 이벤트 정보를 담는 record
     */
    protected record OutboxEventMetadata(String aggregateType, String aggregateId, String topic) {}
}
