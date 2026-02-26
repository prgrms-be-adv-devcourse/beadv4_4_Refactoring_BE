package com.thock.back.settlement.reconciliation.app.useCase;

import com.thock.back.settlement.reconciliation.app.service.ProcessedEventDeduplicator;
import com.thock.back.settlement.reconciliation.in.dto.OrderItemMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceiveOrderItemUseCase {

    private static final String SOURCE_API = "API";
    private static final String SOURCE_KAFKA = "KAFKA";

    private final ProcessedEventDeduplicator processedEventDeduplicator;
    private final SaveSalesLogUseCase saveSalesLogUseCase;

    @Transactional
    public void executeFromApi(OrderItemMessageDto dto) {
        execute(dto, SOURCE_API);
    }

    @Transactional
    public void executeFromKafka(OrderItemMessageDto dto) {
        execute(dto, SOURCE_KAFKA);
    }

    private void execute(OrderItemMessageDto dto, String source) {
        if (processedEventDeduplicator.isDuplicateAndMark(dto, source)) {
            return;
        }
        saveSalesLogUseCase.execute(dto);
    }
}
