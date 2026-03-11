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
    public static final String MARKET_ORDER_BEFORE_PAYMENT_REQUEST_CANCELED = "market.order.before.payment.request.canceled"; // Listener : payment, 결제 전 취소 알림
    public static final String MARKET_ORDER_DELETED = "market.order.deleted"; //  Listener : payment, Payment 삭제 알림
    public static final String MARKET_ORDER_SETTLEMENT = "market.order.settlement"; // Listener : settlement, 정산 이벤트 (결제완료/구매확정/환불완료)
    public static final String MARKET_ORDER_STOCK_CHANGED = "market.order.stock.changed"; // Listener : product, 재고 예약/해제/확정 이벤트

    // Product events
    public static final String PRODUCT_CHANGED = "product.changed";

    // Payment events
    public static final String PAYMENT_REFUND_COMPLETED = "payment.refund.completed";
    public static final String PAYMENT_COMPLETED = "payment.completed";

    // Settlement events
    public static final String SETTLEMENT_COMPLETED = "settlement.completed";

    // Dead Letter Queue (DLQ) topics
    public static final String MARKET_ORDER_STOCK_CHANGED_DLQ = "market.order.stock.changed.dlq";
}
