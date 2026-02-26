package com.thock.back.payment.in.idempotency;

import com.thock.back.shared.market.event.MarketOrderBeforePaymentCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentCompletedEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestedEvent;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import com.thock.back.shared.settlement.event.SettlementCompletedEvent;
import org.springframework.stereotype.Component;

@Component
public class PaymentInboundEventIdempotencyKeyResolver {

    public String memberJoined(MemberJoinedEvent event) {
        return String.join(":",
                "member-joined",
                String.valueOf(event.member().id())
        );
    }

    public String marketOrderPaymentRequested(MarketOrderPaymentRequestedEvent event) {
        return String.join(":",
                "market-order-payment-requested",
                String.valueOf(event.order().id()),
                event.order().orderNumber(),
                String.valueOf(event.pgAmount())
        );
    }

    public String marketOrderPaymentCompleted(MarketOrderPaymentCompletedEvent event) {
        return String.join(":",
                "market-order-payment-completed",
                String.valueOf(event.order().id()),
                event.order().orderNumber()
        );
    }

    public String marketOrderPaymentRequestCanceled(MarketOrderPaymentRequestCanceledEvent event) {
        return String.join(":",
                "market-order-payment-request-canceled",
                event.dto().orderId(),
                String.valueOf(event.dto().amount()),
                String.valueOf(event.dto().cancelReason())
        );
    }

    public String marketOrderBeforePaymentCanceled(MarketOrderBeforePaymentCanceledEvent event) {
        return String.join(":",
                "market-order-before-payment-canceled",
                event.dto().orderId()
        );
    }

    public String settlementCompleted(SettlementCompletedEvent event) {
        return String.join(":",
                "settlement-completed",
                String.valueOf(event.memberID()),
                String.valueOf(event.amount())
        );
    }
}
