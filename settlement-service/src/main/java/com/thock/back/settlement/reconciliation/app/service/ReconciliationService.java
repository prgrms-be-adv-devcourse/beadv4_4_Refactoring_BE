package com.thock.back.settlement.reconciliation.app.service;

import com.thock.back.settlement.reconciliation.app.port.SettlementCandidate;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


// 대사 끝난 정산 후보군을 정산 모듈로 보내주는 서비스
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
