package com.thock.back.api.boundedContext.market.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import static jakarta.persistence.FetchType.LAZY;
@Entity
@Table(name = "market_order_items")
@Getter
@NoArgsConstructor
public class OrderItem extends BaseIdAndTime {
    @ManyToOne(fetch = LAZY)
    private Order order;

    // 상품 정보 스냅샷 (주문 시점 정보 저장)
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Long price;
    private Long salePrice;
    private Integer quantity;

    // 수수료 정책
    private Double payoutRate;

    // 계산된 가격 정보
    private Long totalPrice;
    private Long totalSalePrice;
    private Long discountAmount;
    private Long payoutAmount;      // 판매자 정산 금액
    private Long feeAmount;         // 플랫폼 수수료

    public OrderItem(Order order, Long productId, String productName,
                     String productImageUrl, Long price, Long salePrice,
                     Integer quantity) {
        this.order = order;
        // 스냅샷 저장
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.price = price;
        this.salePrice = salePrice;
        this.quantity = quantity;
        // 수수료 정책 - 전역 설정
        this.payoutRate = MarketPolicy.PRODUCT_PAYOUT_RATE;
        // 계산된 값 자동 설정
        this.totalPrice = quantity * price;
        this.totalSalePrice = quantity * salePrice;
        this.discountAmount = this.totalPrice - this.totalSalePrice;
        this.payoutAmount = MarketPolicy.calculateSalePriceWithoutFee(this.totalSalePrice, payoutRate);
        this.feeAmount = MarketPolicy.calculatePayoutFee(this.totalSalePrice, payoutRate);
    }

}
