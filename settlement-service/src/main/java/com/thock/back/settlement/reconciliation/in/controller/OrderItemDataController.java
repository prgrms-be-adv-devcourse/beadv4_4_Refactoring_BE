package com.thock.back.settlement.reconciliation.in.controller;

import com.thock.back.settlement.reconciliation.app.ReconciliationFacade;
import com.thock.back.settlement.reconciliation.in.dto.OrderItemMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderItemDataController {

    private final ReconciliationFacade reconciliationFacade;

    // 주문 이벤트 데이터 적재 (다른 모듈 이벤트 수신을 수동 호출로 대체)
    @PostMapping("/api/v1/settlements/reconciliation/order-item")
    public void receiveOrderItem(@RequestBody OrderItemMessageDto dto) {
        reconciliationFacade.receiveOrderItems(dto);
    }
}
