package com.thock.back.global.outbox;

import com.thock.back.shared.market.event.*;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import com.thock.back.shared.member.event.MemberModifiedEvent;
import com.thock.back.shared.payment.event.PaymentCompletedEvent;
import com.thock.back.shared.payment.event.PaymentRefundCompletedEvent;
import com.thock.back.shared.settlement.event.SettlementCompletedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 이벤트 타입 이름과 Class 객체 간의 매핑 레지스트리
 * OutboxScheduler에서 역직렬화 시 사용
 * TODO : ? 를 써도 되는거 맞나?
 */
public class OutboxEventTypeRegistry {

    private static final Map<String, Class<?>> EVENT_TYPE_MAP = new HashMap<>();

    static {
        // Member events
        EVENT_TYPE_MAP.put("MemberJoinedEvent", MemberJoinedEvent.class);
        EVENT_TYPE_MAP.put("MemberModifiedEvent", MemberModifiedEvent.class);

        // Market events
        EVENT_TYPE_MAP.put("MarketOrderPaymentRequestedEvent", MarketOrderPaymentRequestedEvent.class);
        EVENT_TYPE_MAP.put("MarketOrderPaymentCompletedEvent", MarketOrderPaymentCompletedEvent.class);
        EVENT_TYPE_MAP.put("MarketOrderPaymentRequestCanceledEvent", MarketOrderPaymentRequestCanceledEvent.class);
        EVENT_TYPE_MAP.put("MarketOrderBeforePaymentCanceledEvent", MarketOrderBeforePaymentCanceledEvent.class);
        EVENT_TYPE_MAP.put("MarketMemberCreatedEvent", MarketMemberCreatedEvent.class);
        EVENT_TYPE_MAP.put("MarketOrderDeletedEvent", MarketOrderDeletedEvent.class);
        EVENT_TYPE_MAP.put("MarketOrderSettlementEvent", MarketOrderSettlementEvent.class);

        // Payment events
        EVENT_TYPE_MAP.put("PaymentCompletedEvent", PaymentCompletedEvent.class);
        EVENT_TYPE_MAP.put("PaymentRefundCompletedEvent", PaymentRefundCompletedEvent.class);

        // Settlement events
        EVENT_TYPE_MAP.put("SettlementCompletedEvent", SettlementCompletedEvent.class);
    }

    /**
     * 이벤트 타입 이름으로 Class 객체 조회
     *
     * @param eventType 이벤트 타입 이름 (예: "MemberJoinedEvent")
     * @return 해당 Class 객체
     * @throws IllegalArgumentException 알 수 없는 이벤트 타입인 경우
     */
    public static Class<?> getEventClass(String eventType) {
        Class<?> clazz = EVENT_TYPE_MAP.get(eventType);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
        return clazz;
    }

    /**
     * 이벤트 타입이 등록되어 있는지 확인
     */
    public static boolean isRegistered(String eventType) {
        return EVENT_TYPE_MAP.containsKey(eventType);
    }

    /**
     * 새 이벤트 타입 등록 (확장용)
     */
    public static void register(String eventType, Class<?> eventClass) {
        EVENT_TYPE_MAP.put(eventType, eventClass);
    }
}
