package com.thock.back.api.boundedContext.payment.in;

import com.thock.back.api.boundedContext.payment.app.PaymentFacade;
import com.thock.back.api.shared.market.event.MarketOrderPaymentRequestedEvent;
import com.thock.back.api.shared.member.event.MemberJoinedEvent;
import com.thock.back.api.shared.member.event.MemberModifiedEvent;
import com.thock.back.api.shared.payment.event.PaymentAddBalanceLogEvent;
import com.thock.back.api.shared.payment.event.PaymentAddPaymentLogEvent;
import com.thock.back.api.shared.payment.event.PaymentAddRevenueLogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    private final PaymentFacade paymentFacade;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(MemberJoinedEvent event) {
        paymentFacade.syncMember(event.getMember());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(MemberModifiedEvent event) {
        paymentFacade.syncMember(event.getMember());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(MarketOrderPaymentRequestedEvent event) {
        paymentFacade.requestedOrderPayment(event.getOrder(), event.getPgAmount(), event.getPaymentKey());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(PaymentAddRevenueLogEvent event) {
        paymentFacade.addRevenueLog(event.getWallet(), event.getEventType(), event.getAmount());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(PaymentAddBalanceLogEvent event) {
        paymentFacade.addBalanceLog(event.getWallet(), event.getEventType(), event.getAmount());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(PaymentAddPaymentLogEvent event) {
        paymentFacade.addPaymentLog(event.getPayment());
    }
}
