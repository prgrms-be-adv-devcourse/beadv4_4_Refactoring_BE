package com.thock.back.market.in;


import com.thock.back.market.app.MarketFacade;
import com.thock.back.shared.market.event.MarketMemberCreatedEvent;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import com.thock.back.shared.member.event.MemberModifiedEvent;
import com.thock.back.shared.payment.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
public class MarketEventListener {
    private final MarketFacade marketFacade;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(MemberJoinedEvent event){
        marketFacade.syncMember(event.getMember());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(MemberModifiedEvent event){
        marketFacade.syncMember(event.getMember());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(MarketMemberCreatedEvent event){
        marketFacade.createCart(event.getMember());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(PaymentCompletedEvent event){
        String orderId = event.getPayment().getOrderId();
        marketFacade.completeOrderPayment(orderId);
    }


}
