package com.thock.back.market.in;


import com.thock.back.market.app.MarketFacade;
import com.thock.back.shared.market.event.MarketMemberCreatedEvent;
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
public class MarketEventListener {
    private final MarketFacade marketFacade;

    /**
     *  Market Member 생성 시 Cart가 생성됨.
     *  이건 market-service 내부에서 일어나는 작업이므로 Spring Event로 충분함
     */
    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(MarketMemberCreatedEvent event){
        log.info("Received MarketMemberCreatedEvent via Spring Event: memberId={}", event.member().id());
        marketFacade.createCart(event.member());
    }

}
