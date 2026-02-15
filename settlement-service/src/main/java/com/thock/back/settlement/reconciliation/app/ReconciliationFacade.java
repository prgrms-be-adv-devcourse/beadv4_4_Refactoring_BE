package com.thock.back.settlement.reconciliation.app;

import com.thock.back.settlement.reconciliation.app.UseCase.RunReconciliationUseCase;
import com.thock.back.settlement.reconciliation.app.UseCase.SavePgDataUseCase;
import com.thock.back.settlement.reconciliation.app.UseCase.SaveSalesLogUseCase;
import com.thock.back.settlement.reconciliation.in.dto.OrderItemMessageDto;
import com.thock.back.settlement.reconciliation.in.dto.PgSalesDto;
import com.thock.back.shared.settlement.dto.SettlementOrderItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationFacade {
    private final SaveSalesLogUseCase saveSalesLogUseCase;
    private final SavePgDataUseCase savePgDataUseCase;
    private final RunReconciliationUseCase runReconciliationUseCase;

//  --퍼싸드에선 pg 데이터 저장, 주문서 저장, 대사 진행 세가지의 메소드만 있으면 됨--

    // 1. 주문서를 받아와서 저장하는 로직(결제 완료의 주문서, 환불의 주문서)
    @Transactional
    public void receiveOrderItems(OrderItemMessageDto dto) {
        saveSalesLogUseCase.execute(dto);
    }

    @Transactional
    public void receiveSettlementItems(List<SettlementOrderItemDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (SettlementOrderItemDto item : items) {
            saveSalesLogUseCase.execute(toOrderItemMessageDto(item));
        }
    }
    // 2. PG사의 주문서 저장하는 로직
    @Transactional
    public void receivePgData(List<PgSalesDto> dtos) {
        savePgDataUseCase.execute(dtos);
    }
    // 3. 1번과 2번의 데이터가 일치하는지 검증하는 로직
    @Transactional
    public void runReconciliation(LocalDate targetDate) {
        runReconciliationUseCase.execute(targetDate);
    }

    private OrderItemMessageDto toOrderItemMessageDto(SettlementOrderItemDto item) {
        return new OrderItemMessageDto(
                item.orderNo(),
                item.sellerId(),
                item.productId(),
                item.productName(),
                item.productQuantity(),
                item.productPrice(),
                item.paymentAmount(),
                item.eventType(),
                item.metadata(),
                item.snapshotAt()
        );
    }
}
