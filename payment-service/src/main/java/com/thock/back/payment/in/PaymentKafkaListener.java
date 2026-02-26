package com.thock.back.payment.in;

import com.thock.back.global.inbox.InboxGuard;
import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.payment.app.PaymentFacade;
import com.thock.back.payment.in.idempotency.PaymentInboundEventIdempotencyKeyResolver;
import com.thock.back.shared.market.event.MarketOrderBeforePaymentCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentCompletedEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestedEvent;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import com.thock.back.shared.member.event.MemberModifiedEvent;
import com.thock.back.shared.settlement.event.SettlementCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaListener {
    private static final String PAYMENT_CONSUMER_GROUP = "payment-service";

    private final PaymentFacade paymentFacade;
    private final ObjectProvider<InboxGuard> inboxGuardProvider;
    private final PaymentInboundEventIdempotencyKeyResolver keyResolver;

    @KafkaListener(topics = KafkaTopics.MEMBER_JOINED, groupId = "payment-service")
    @Transactional
    public void handle(MemberJoinedEvent event) {
        if (!shouldProcess(KafkaTopics.MEMBER_JOINED, keyResolver.memberJoined(event))) {
            return;
        }
        log.info("Received MemberJoinedEvent via Kafka: memberId={}", event.member().id());
        paymentFacade.syncMember(event.member());
    }

    @KafkaListener(topics = KafkaTopics.MEMBER_MODIFIED, groupId = "payment-service")
    @Transactional
    public void handle(MemberModifiedEvent event) {
        log.info("Received MemberModifiedEvent via Kafka: memberId={}", event.member().id());
        paymentFacade.syncMember(event.member());
    }

    @KafkaListener(topics = KafkaTopics.MARKET_ORDER_PAYMENT_REQUESTED, groupId = "payment-service")
    @Transactional
    public void handle(MarketOrderPaymentRequestedEvent event) {
        if (!shouldProcess(KafkaTopics.MARKET_ORDER_PAYMENT_REQUESTED, keyResolver.marketOrderPaymentRequested(event))) {
            return;
        }
        log.info("Received MarketOrderPaymentRequestedEvent via Kafka: orderId={}", event.order().id());
        paymentFacade.requestedOrderPayment(event.order(), event.pgAmount());
    }

    @KafkaListener(topics = KafkaTopics.MARKET_ORDER_PAYMENT_COMPLETED, groupId = "payment-service")
    @Transactional
    public void handle(MarketOrderPaymentCompletedEvent event) {
        if (!shouldProcess(KafkaTopics.MARKET_ORDER_PAYMENT_COMPLETED, keyResolver.marketOrderPaymentCompleted(event))) {
            return;
        }
        log.info("Received MarketOrderPaymentCompletedEvent via Kafka: orderId={}", event.order().id());
        paymentFacade.completedOrderPayment(event.order());
    }

    @KafkaListener(topics = KafkaTopics.MARKET_ORDER_PAYMENT_REQUEST_CANCELED, groupId = "payment-service")
    @Transactional
    public void handle(MarketOrderPaymentRequestCanceledEvent event) {
        if (!shouldProcess(
                KafkaTopics.MARKET_ORDER_PAYMENT_REQUEST_CANCELED,
                keyResolver.marketOrderPaymentRequestCanceled(event)
        )) {
            return;
        }
        log.info("Received MarketOrderPaymentRequestCanceledEvent via Kafka : orderId={}", event.dto().orderId());
        paymentFacade.canceledPayment(event.dto());
    }

    @KafkaListener(topics = KafkaTopics.SETTLEMENT_COMPLETED, groupId = "payment-service")
    @Transactional
    public void handle(SettlementCompletedEvent event) {
        if (!shouldProcess(KafkaTopics.SETTLEMENT_COMPLETED, keyResolver.settlementCompleted(event))) {
            return;
        }
        log.info("Received SettlementCompletedEvent via Kafka: memberId={}, amount={}", event.memberID(), event.amount());
        paymentFacade.completeSettlementPayment(event.memberID(), event.amount());
    }

    @KafkaListener(topics = KafkaTopics.MARKET_ORDER_BEFORE_PAYMENT_REQUEST_CANCELED, groupId = "payment-service")
    @Transactional
    public void handle(MarketOrderBeforePaymentCanceledEvent event) {
        if (!shouldProcess(
                KafkaTopics.MARKET_ORDER_BEFORE_PAYMENT_REQUEST_CANCELED,
                keyResolver.marketOrderBeforePaymentCanceled(event)
        )) {
            return;
        }
        log.info("Received MarketOrderBeforePaymentCanceledEvent via Kafka: orderId={}", event.dto().orderId());
        paymentFacade.canceledBeforePayment(event.dto().orderId());
    }

    private boolean shouldProcess(String topic, String idempotencyKey) {
        InboxGuard inboxGuard = inboxGuardProvider.getIfAvailable();
        if (inboxGuard == null) {
            return true;
        }
        return inboxGuard.tryClaim(idempotencyKey, topic, PAYMENT_CONSUMER_GROUP);
    }
}
