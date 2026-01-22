package com.thock.back.api.boundedContext.settlement.in;

import com.thock.back.api.boundedContext.settlement.app.SettlementFacade;
import com.thock.back.api.boundedContext.settlement.out.SettlementMemberRepository;
import com.thock.back.api.shared.member.event.SellerRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SettlementEventListener {
    private final SettlementFacade settlementFacade;
//TODO:
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(SellerRegisteredEvent event) {settlementFacade.sync}
}
