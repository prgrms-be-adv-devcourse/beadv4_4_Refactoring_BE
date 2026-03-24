package com.thock.back.shared.market.event;

import com.thock.back.shared.settlement.dto.SettlementOrderItemDto;

import java.util.List;

/**
 * Market → Settlement 정산 이벤트
 * 결제 완료, 구매 확정, 환불 완료 시 발행
 * eventType으로 구분: PAYMENT_COMPLETED, PURCHASE_CONFIRMED, REFUND_COMPLETED
 */
public record MarketOrderSettlementEvent(
        List<SettlementOrderItemDto> items
) {
}
