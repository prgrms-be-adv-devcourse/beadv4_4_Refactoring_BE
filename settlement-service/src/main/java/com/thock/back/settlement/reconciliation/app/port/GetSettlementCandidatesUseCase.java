package com.thock.back.settlement.reconciliation.app.port;

import java.util.List;

public interface GetSettlementCandidatesUseCase {
    List<SettlementCandidate> getCandidates();
}