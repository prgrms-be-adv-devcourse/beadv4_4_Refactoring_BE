package com.thock.back.payment.in;

import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.payment.app.PaymentFacade;
import com.thock.back.shared.market.event.MarketOrderPaymentCompletedEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestedEvent;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import com.thock.back.shared.member.event.MemberModifiedEvent;
import com.thock.back.shared.settlement.event.SettlementCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaListener {
    private final PaymentFacade paymentFacade;

    @KafkaListener(topics = KafkaTopics.MEMBER_JOINED, groupId = "payment-service")
    @Transactional
    public void handle(MemberJoinedEvent event) {
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
        log.info("Received MarketOrderPaymentRequestedEvent via Kafka: orderId={}", event.order().id());
        paymentFacade.requestedOrderPayment(event.order(), event.pgAmount());
    }

    @KafkaListener(topics = KafkaTopics.MARKET_ORDER_PAYMENT_COMPLETED, groupId = "payment-service")
    @Transactional
    public void handle(MarketOrderPaymentCompletedEvent event) {
        log.info("Received MarketOrderPaymentCompletedEvent via Kafka: orderId={}", event.order().id());
        paymentFacade.completedOrderPayment(event.order());
    }

    @KafkaListener(topics = KafkaTopics.MARKET_ORDER_PAYMENT_REQUEST_CANCELED, groupId = "payment-service")
    @Transactional
    public void handle(MarketOrderPaymentRequestCanceledEvent event) {
        log.info("Received MarketOrderPaymentRequestCanceledEvent via Kafka");
        paymentFacade.canceledPayment(event.dto());
    }

    @KafkaListener(topics = KafkaTopics.SETTLEMENT_COMPLETED, groupId = "payment-service")
    @Transactional
    public void handle(SettlementCompletedEvent event) {
        log.info("Received SettlementCompletedEvent via Kafka: memberId={}, amount={}", event.memberID(), event.amount());
        paymentFacade.completeSettlementPayment(event.memberID(), event.amount());
    }
}