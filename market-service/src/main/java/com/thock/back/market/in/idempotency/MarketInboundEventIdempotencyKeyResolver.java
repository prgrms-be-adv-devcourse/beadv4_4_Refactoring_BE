package com.thock.back.market.in.idempotency;

import com.thock.back.shared.member.event.MemberJoinedEvent;
import com.thock.back.shared.member.event.MemberModifiedEvent;
import com.thock.back.shared.payment.event.PaymentCompletedEvent;
import com.thock.back.shared.payment.event.PaymentRefundCompletedEvent;
import org.springframework.stereotype.Component;

@Component
public class MarketInboundEventIdempotencyKeyResolver {

    public String memberJoined(MemberJoinedEvent event) {
        return String.join(":",
                "member-joined",
                String.valueOf(event.member().id())
        );
    }

    public String memberModified(MemberModifiedEvent event) {
        return String.join(":",
                "member-modified",
                String.valueOf(event.member().id()),
                String.valueOf(event.member().updatedAt())
        );
    }

    public String paymentCompleted(PaymentCompletedEvent event) {
        return String.join(":",
                "payment-completed",
                event.payment().orderId(),
                String.valueOf(event.payment().id())
        );
    }

    public String paymentRefundCompleted(PaymentRefundCompletedEvent event) {
        return String.join(":",
                "payment-refund-completed",
                event.dto().orderId(),
                String.valueOf(event.dto().amount())
        );
    }
}
