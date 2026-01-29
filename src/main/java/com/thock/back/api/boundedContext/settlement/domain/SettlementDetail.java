package com.thock.back.api.boundedContext.settlement.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "settlement_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id")
    private Settlement settlement;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_item_id")
    private Long orderItemId; // Market의 OrderItem PK

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "quantity")
    private Integer quantity; // [필수] OrderItem.quantity

    // --- 금액 정보 (Long) ---

    @Column(name = "payment_amount")
    private Long paymentAmount; // OrderItem.totalSalePrice (결제된 금액)

    @Column(name = "payout_amount")
    private Long payoutAmount;  // OrderItem.payoutAmount (판매자 지급액)

    @Column(name = "fee")
    private Long fee;           // OrderItem.feeAmount (수수료)

    @Builder
    public SettlementDetail(Long orderId, Long orderItemId, Long productId, String productName,
                            Integer quantity, Long paymentAmount, Long payoutAmount, Long fee) {
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.paymentAmount = paymentAmount;
        this.payoutAmount = payoutAmount;
        this.fee = fee;
    }

    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
    }
}