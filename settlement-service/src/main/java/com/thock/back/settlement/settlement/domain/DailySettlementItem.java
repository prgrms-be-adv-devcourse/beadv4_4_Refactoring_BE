package com.thock.back.settlement.settlement.domain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "finance_settlement_daily_item") // 상세 테이블
public class DailySettlementItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_settlement_id")
    private DailySettlement dailySettlement;

    private String orderNo;         // 주문 번호 (상세 내역용)
    private Long itemId;            // 상품 ID
    private Long paymentAmount;     // 결제 금액 (개별)

    @Builder
    public DailySettlementItem(DailySettlement dailySettlement, String orderNo, Long itemId, Long paymentAmount) {
        this.dailySettlement = dailySettlement;
        this.orderNo = orderNo;
        this.itemId = itemId;
        this.paymentAmount = paymentAmount;
    }
}