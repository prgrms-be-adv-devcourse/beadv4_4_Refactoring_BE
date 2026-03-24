package com.thock.back.payment.in;

import com.thock.back.global.inbox.InboxGuard;
import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.payment.app.PaymentFacade;
import com.thock.back.payment.in.idempotency.PaymentInboundEventIdempotencyKeyResolver;
import com.thock.back.shared.market.dto.OrderDto;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestedEvent;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import com.thock.back.shared.member.dto.MemberDto;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentKafkaListenerTest {

    @Mock
    private PaymentFacade paymentFacade;

    @Mock
    private ObjectProvider<InboxGuard> inboxGuardProvider;

    @Mock
    private InboxGuard inboxGuard;

    @Mock
    private PaymentInboundEventIdempotencyKeyResolver keyResolver;

    private PaymentKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentKafkaListener(paymentFacade, inboxGuardProvider, keyResolver);
    }

    @Test
    @DisplayName("inbox enabled + claim 성공 시 비즈니스 로직 실행")
    void handleMemberJoined_whenClaimed_shouldProcess() {
        MemberJoinedEvent event = memberJoinedEvent(1L);
        when(keyResolver.memberJoined(event)).thenReturn("member-joined:1");
        when(inboxGuardProvider.getIfAvailable()).thenReturn(inboxGuard);
        when(inboxGuard.tryClaim("member-joined:1", KafkaTopics.MEMBER_JOINED, "payment-service"))
                .thenReturn(true);

        listener.handle(event);

        verify(paymentFacade).syncMember(event.member());
    }

    @Test
    @DisplayName("inbox enabled + 중복(claim 실패) 시 비즈니스 로직 미실행")
    void handleMemberJoined_whenDuplicated_shouldSkip() {
        MemberJoinedEvent event = memberJoinedEvent(1L);
        when(keyResolver.memberJoined(event)).thenReturn("member-joined:1");
        when(inboxGuardProvider.getIfAvailable()).thenReturn(inboxGuard);
        when(inboxGuard.tryClaim("member-joined:1", KafkaTopics.MEMBER_JOINED, "payment-service"))
                .thenReturn(false);

        listener.handle(event);

        verify(paymentFacade, never()).syncMember(any());
    }

    @Test
    @DisplayName("inbox disabled 시 claim 없이 기존 로직 실행")
    void handlePaymentRequested_whenInboxDisabled_shouldProcess() {
        MarketOrderPaymentRequestedEvent event = new MarketOrderPaymentRequestedEvent(
                new OrderDto(10L, 1L, "buyer", "ORDER-1", 10000L),
                3000L
        );
        when(keyResolver.marketOrderPaymentRequested(event))
                .thenReturn("market-order-payment-requested:10:ORDER-1:3000");
        when(inboxGuardProvider.getIfAvailable()).thenReturn(null);

        listener.handle(event);

        verify(paymentFacade).requestedOrderPayment(event.order(), event.pgAmount());
        verifyNoInteractions(inboxGuard);
    }

    private MemberJoinedEvent memberJoinedEvent(Long memberId) {
        return new MemberJoinedEvent(
                new MemberDto(
                        memberId,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        "member@test.com",
                        "member",
                        MemberRole.USER,
                        MemberState.ACTIVE,
                        null,
                        null,
                        null
                )
        );
    }
}
