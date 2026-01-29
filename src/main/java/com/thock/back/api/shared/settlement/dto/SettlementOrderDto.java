package com.thock.back.api.shared.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementOrderDto {
    // 1. 식별자
    private Long orderId;
    private Long orderItemId; // OrderItem의 ID (상세 대조용)

    // 2. 상품 정보
    private Long productId;
    private String productName;
    private Integer quantity; // 수량 (OrderItem에 있어서 추가함)

    // 3. 금액 정보 (OrderItem 필드명과 일치시킴 -> 매핑 실수 방지)
    private Long totalSalePrice;  // 결제 금액 (할인 적용 후 실 판매가)
    private Long payoutAmount;    // 판매자 정산 금액 (이미 수수료 떼인 값)
    private Long feeAmount;       // 수수료

    // 4. 시간 정보
    private LocalDateTime confirmedAt; // 구매 확정일 (쿼리용)
}