package com.thock.back.payment.in;

import com.thock.back.payment.app.PaymentFacade;
import com.thock.back.payment.out.event.PaymentAddBalanceLogEvent;
import com.thock.back.payment.out.event.PaymentAddPaymentLogEvent;
import com.thock.back.payment.out.event.PaymentAddRevenueLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {
    private final PaymentFacade paymentFacade;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(PaymentAddRevenueLogEvent event) {
        log.info("Received PaymentAddRevenueLogEvent event = {}", event);
        paymentFacade.addRevenueLog(event.wallet(), event.eventType(), event.amount());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(PaymentAddBalanceLogEvent event) {
        log.info("Received PaymentAddBalanceLogEvent event = {}", event);
        paymentFacade.addBalanceLog(event.wallet(), event.eventType(), event.amount());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(PaymentAddPaymentLogEvent event) {
        log.info("Received PaymentAddPaymentLogEvent event = {}", event);
        paymentFacade.addPaymentLog(event.payment());
    }
}