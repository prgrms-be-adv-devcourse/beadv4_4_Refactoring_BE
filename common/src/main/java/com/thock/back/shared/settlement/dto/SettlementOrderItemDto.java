package com.thock.back.shared.settlement.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Market → Settlement 이벤트용 DTO
 * Settlement의 OrderItemMessageDto와 호환되는 형식
 */
public record SettlementOrderItemDto(
        String orderNo, // 주문 번호
        Long sellerId, // 판매자 ID
        Long productId, // 상품 ID
        String productName, // 상품 이름
        int productQuantity, // 상품 수량
        // TODO : 나중에는 정산에서도 정가, 할인가 다 받아야하지 않을까 싶음
        Long productPrice,      // 상품 개당 가격 -> 할인가(실 판매가)
        Long paymentAmount,     // 결제 총액(부분환불, 부분 확정시에 조심해서 담아주세요. 가격*수량) -> 실 판매가
        String eventType,       // PAYMENT_COMPLETED, PURCHASE_CONFIRMED, REFUND_COMPLETED
        Map<String, Object> metadata, // 환불 사유 등을 담는 JSON 필드
        /**
         * market모듈에서 주문서 생성 당시 시간
         * Order에서 정산으로 보내줄건데 상태가 update 될 때 이니까 updatedAt == snapshotAt
         */
        LocalDateTime snapshotAt
) {
}
