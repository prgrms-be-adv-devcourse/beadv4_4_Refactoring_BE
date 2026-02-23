package com.thock.back.settlement.reconciliation.app;

import com.thock.back.settlement.reconciliation.app.useCase.ReceiveOrderItemUseCase;
import com.thock.back.settlement.reconciliation.app.useCase.ReceiveSettlementItemsUseCase;
import com.thock.back.settlement.reconciliation.app.useCase.RunReconciliationUseCase;
import com.thock.back.settlement.reconciliation.app.useCase.SavePgDataUseCase;
import com.thock.back.settlement.reconciliation.in.dto.OrderItemMessageDto;
import com.thock.back.settlement.reconciliation.in.dto.PgSalesDto;
import com.thock.back.shared.settlement.dto.SettlementOrderItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationFacade {
    private final ReceiveOrderItemUseCase receiveOrderItemUseCase;
    private final ReceiveSettlementItemsUseCase receiveSettlementItemsUseCase;
    private final SavePgDataUseCase savePgDataUseCase;
    private final RunReconciliationUseCase runReconciliationUseCase;

//  --퍼싸드에선 pg 데이터 저장, 주문서 저장, 대사 진행 세가지의 메소드만 있으면 됨--

    // 1-1. SaleLog 수동 적재시 (API를 통해 받은 OrderItem dto를 SalesLog로 단건 저장)
    public void receiveOrderItems(OrderItemMessageDto dto) {
        receiveOrderItemUseCase.executeFromApi(dto);
    }

    // 1-2. SalesLog 외부 이벤트를 통해 적재시 (Kafka를 통해 받은 리스트에 담긴 OrderItem을 SalesLog로 여러개 저장)
    public void receiveSettlementItems(List<SettlementOrderItemDto> items) {
        receiveSettlementItemsUseCase.execute(items);
    }
    // 2. 수동으로 PG사의 주문서 저장하는 로직
    public void receivePgData(List<PgSalesDto> dtos) {
        savePgDataUseCase.execute(dtos);
    }
    // 3. 1번과 2번의 데이터가 일치하는지 검증하는 로직
    public void runReconciliation(LocalDate targetDate) {
        runReconciliationUseCase.execute(targetDate);
    }
}
