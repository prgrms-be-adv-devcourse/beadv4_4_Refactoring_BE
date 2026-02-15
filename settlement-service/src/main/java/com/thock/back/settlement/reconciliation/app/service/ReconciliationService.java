package com.thock.back.settlement.reconciliation.app.service;

import com.thock.back.settlement.reconciliation.app.port.SettlementCandidate;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReconciliationService {

    private final SalesLogRepository salesLogRepository;

    public List<SettlementCandidate> findCandidates() {
        return salesLogRepository.findSettlementCandidates().stream()
                .map(log -> new SettlementCandidate(
                        log.getId(),
                        log.getSellerId(),
                        log.getProductId(),
                        log.getProductName(),
                        log.getProductQuantity(),
                        log.getPaymentAmount().amount(),
                        log.getOrderNo()
                ))
                .toList();
    }
}
