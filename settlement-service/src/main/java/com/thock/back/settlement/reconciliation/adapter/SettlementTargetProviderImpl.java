package com.thock.back.settlement.reconciliation.adapter;

import com.thock.back.settlement.reconciliation.domain.enums.SettlementStatus;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import com.thock.back.settlement.shared.reconciliation.dto.SettlementTargetDto;
import com.thock.back.settlement.shared.reconciliation.port.SettlementTargetProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SettlementTargetProviderImpl implements SettlementTargetProvider {

    private final SalesLogRepository salesLogRepository;

    @Override
    public List<SettlementTargetDto> getReadySnapshots(Long id, int size) {
        return salesLogRepository.findAllBySettlementStatus(SettlementStatus.READY).stream()
                .map(entity -> new SettlementTargetDto(
                        entity.getId(),
                        entity.getSellerId(),
                        entity.getPaymentAmount()
                ))
                .toList();
    }
}
