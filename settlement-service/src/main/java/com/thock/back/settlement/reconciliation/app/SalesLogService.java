package com.thock.back.settlement.reconciliation.app;

import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.domain.enums.OrderEventStatus;
import com.thock.back.settlement.reconciliation.in.dto.OrderItemMessageDto;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesLogService {
    private final SalesLogRepository salesLogRepository;

    // 주문서로 받은 모든 내용을 저장
    // 결제완료/환불은 새로운 주문서, 구매확정건의 경우 새로운 주문서가 아니므로 주문서별로 분기처리 해줘야함
    @Transactional
    public void saveSalesLog(OrderItemMessageDto dto){
        OrderEventStatus eventStatus = OrderEventStatus.from(dto.eventType());
        switch (eventStatus) {

            case PAYMENT_COMPLETED, REFUND_COMPLETED -> {
                salesLogRepository.save(dto.toEntity());
            }

            case PURCHASE_CONFIRMED -> {
                salesLogRepository.findByOrderNoAndTransactionType(
                        dto.orderNo(),
                        eventStatus.getTransactionType()
                ).ifPresent(SalesLog::readySettlement);
            }
        }
    }
}