package com.thock.back.settlement.reconciliation.app.useCase;

import com.thock.back.settlement.reconciliation.in.mapper.SettlementOrderItemMapper;
import com.thock.back.shared.settlement.dto.SettlementOrderItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceiveSettlementItemsUseCase {

    private final SettlementOrderItemMapper settlementOrderItemMapper;
    private final ReceiveOrderItemUseCase receiveOrderItemUseCase;

    public void execute(List<SettlementOrderItemDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (SettlementOrderItemDto item : items) {
            receiveOrderItemUseCase.executeFromKafka(
                    settlementOrderItemMapper.toOrderItemMessageDto(item)
            );
        }
    }
}
