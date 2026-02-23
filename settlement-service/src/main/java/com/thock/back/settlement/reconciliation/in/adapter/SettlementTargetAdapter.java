package com.thock.back.settlement.reconciliation.in.adapter;

import com.thock.back.settlement.reconciliation.app.port.GetSettlementCandidatesUseCase;
import com.thock.back.settlement.reconciliation.app.port.SettlementCandidate;
import com.thock.back.settlement.reconciliation.app.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SettlementTargetAdapter implements GetSettlementCandidatesUseCase {

    private final ReconciliationService reconciliationService;

    @Override
    public List<SettlementCandidate> getCandidates() {
        return reconciliationService.findCandidates();
    }
}