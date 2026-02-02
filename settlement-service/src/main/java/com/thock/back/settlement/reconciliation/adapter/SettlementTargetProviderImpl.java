package com.thock.back.settlement.reconciliation.adapter;

import com.thock.back.settlement.reconciliation.out.InternalOrderSnapshotRepository;
import com.thock.back.settlement.shared.reconciliation.dto.SettlementTargetDto;
import com.thock.back.settlement.shared.reconciliation.port.SettlementTargetProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SettlementTargetProviderImpl implements SettlementTargetProvider {

    private final InternalOrderSnapshotRepository internalOrderSnapshotRepository;

    @Override
    public List<SettlementTargetDto> getReadySnapshots(Long id, int size) {
        return internalOrderSnapshotRepository.findAllBySettlementStatus("READY").stream()
                .map(entity -> new SettlementTargetDto(
                        entity.getId(),
                        entity.getSellerId(),
                        entity.getPaymentAmount()
                ))
                .toList();
    }
}
