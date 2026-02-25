package com.thock.back.market.in;

import com.thock.back.global.inbox.InboxGuard;
import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.market.app.MarketFacade;
import com.thock.back.market.in.idempotency.MarketInboundEventIdempotencyKeyResolver;
import com.thock.back.market.monitoring.MarketKafkaInboundMetrics;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import com.thock.back.shared.member.dto.MemberDto;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import com.thock.back.shared.payment.dto.PaymentDto;
import com.thock.back.shared.payment.event.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketKafkaListenerTest {

    private static final String CONSUMER_GROUP = "market-service";

    @Mock
    private MarketFacade marketFacade;
    @Mock
    private ObjectProvider<InboxGuard> inboxGuardProvider;
    @Mock
    private InboxGuard inboxGuard;
    @Mock
    private MarketInboundEventIdempotencyKeyResolver keyResolver;
    @Mock
    private MarketKafkaInboundMetrics inboundMetrics;

    private MarketKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new MarketKafkaListener(marketFacade, inboxGuardProvider, keyResolver, inboundMetrics);
    }

    @Test
    @DisplayName("MemberJoinedEvent 중복이면 syncMember를 호출하지 않는다")
    void handleMemberJoined_duplicate_skips() {
        MemberJoinedEvent event = new MemberJoinedEvent(memberDto(1L));

        when(keyResolver.memberJoined(event)).thenReturn("member-joined:1");
        when(inboxGuardProvider.getIfAvailable()).thenReturn(inboxGuard);
        when(inboxGuard.tryClaim("member-joined:1", KafkaTopics.MEMBER_JOINED, CONSUMER_GROUP))
                .thenReturn(false);

        listener.handle(event);

        verify(marketFacade, never()).syncMember(any());
    }

    @Test
    @DisplayName("MemberJoinedEvent 최초 수신이면 syncMember를 호출한다")
    void handleMemberJoined_claimed_processes() {
        MemberJoinedEvent event = new MemberJoinedEvent(memberDto(2L));

        when(keyResolver.memberJoined(event)).thenReturn("member-joined:2");
        when(inboxGuardProvider.getIfAvailable()).thenReturn(inboxGuard);
        when(inboxGuard.tryClaim("member-joined:2", KafkaTopics.MEMBER_JOINED, CONSUMER_GROUP))
                .thenReturn(true);

        listener.handle(event);

        verify(marketFacade).syncMember(eq(event.member()));
    }

    @Test
    @DisplayName("PaymentCompletedEvent 중복이면 completeOrderPayment를 호출하지 않는다")
    void handlePaymentCompleted_duplicate_skips() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(paymentDto(10L, "ORDER-TEST-1"));

        when(keyResolver.paymentCompleted(event)).thenReturn("payment-completed:ORDER-TEST-1:10");
        when(inboxGuardProvider.getIfAvailable()).thenReturn(inboxGuard);
        when(inboxGuard.tryClaim(
                "payment-completed:ORDER-TEST-1:10",
                KafkaTopics.PAYMENT_COMPLETED,
                CONSUMER_GROUP
        )).thenReturn(false);

        listener.handle(event);

        verify(marketFacade, never()).completeOrderPayment(any());
    }

    @Test
    @DisplayName("PaymentCompletedEvent 최초 수신이면 completeOrderPayment를 호출한다")
    void handlePaymentCompleted_claimed_processes() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(paymentDto(11L, "ORDER-TEST-2"));

        when(keyResolver.paymentCompleted(event)).thenReturn("payment-completed:ORDER-TEST-2:11");
        when(inboxGuardProvider.getIfAvailable()).thenReturn(inboxGuard);
        when(inboxGuard.tryClaim(
                "payment-completed:ORDER-TEST-2:11",
                KafkaTopics.PAYMENT_COMPLETED,
                CONSUMER_GROUP
        )).thenReturn(true);

        listener.handle(event);

        verify(marketFacade).completeOrderPayment("ORDER-TEST-2");
    }

    private MemberDto memberDto(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        return new MemberDto(
                memberId,
                now,
                now,
                "user" + memberId + "@example.com",
                "user" + memberId,
                MemberRole.USER,
                MemberState.ACTIVE,
                null,
                null,
                null
        );
    }

    private PaymentDto paymentDto(Long paymentId, String orderId) {
        return new PaymentDto(
                paymentId,
                orderId,
                "pk-test-" + paymentId,
                1L,
                1000L,
                1000L,
                LocalDateTime.now(),
                0L
        );
    }
}
