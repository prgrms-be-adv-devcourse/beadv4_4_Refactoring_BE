package com.thock.back.global.kafka;

/**
 * 토픽 이름 : 누가 발행하는가 기준으로 작성한다.
 */
public class KafkaTopics {
    // Member events
    public static final String MEMBER_JOINED = "member.joined";
    public static final String MEMBER_MODIFIED = "member.modified";


    // Market events
    public static final String MARKET_ORDER_PAYMENT_REQUESTED = "market.order.payment.requested"; // Listener : payment, PG 결제 필요
    public static final String MARKET_ORDER_PAYMENT_COMPLETED = "market.order.payment.completed"; // Listener : payment, 예치금으로만 결제 가능
    public static final String MARKET_ORDER_PAYMENT_REQUEST_CANCELED = "market.order.payment.request.canceled"; // Listener : payment, 환불 요청
    public static final String MARKET_ORDER_BEFORE_PAYMENT_REQUEST_CANCELED = "market.order.before.payment.request.canceled";

    // Payment events
    public static final String PAYMENT_REFUND_COMPLETED = "payment.refund.completed";

    // Settlement events
    public static final String SETTLEMENT_COMPLETED = "settlement.completed";

}
