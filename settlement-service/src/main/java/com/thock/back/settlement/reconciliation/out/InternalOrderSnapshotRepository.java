package com.thock.back.settlement.reconciliation.out;

import com.thock.back.settlement.reconciliation.domain.InternalOrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InternalOrderSnapshotRepository extends JpaRepository<InternalOrderSnapshot, Long> {
    List<InternalOrderSnapshot> findAllBySettlementStatus(String settlementStatus);
}
