package com.thock.back.settlement.reconciliation.in;

import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.settlement.reconciliation.app.ReconciliationFacade;
import com.thock.back.shared.market.event.MarketOrderSettlementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationKafkaListener {

    private final ReconciliationFacade reconciliationFacade;

    @KafkaListener(topics = KafkaTopics.MARKET_ORDER_SETTLEMENT, groupId = "settlement-service")
    @Transactional
    public void handle(MarketOrderSettlementEvent event) {
        int size = event.items() == null ? 0 : event.items().size();
        log.info("Received MarketOrderSettlementEvent via Kafka: items={}", size);
        reconciliationFacade.receiveSettlementItems(event.items());
    }
}
