package com.thock.back.market.domain;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
@Entity
@Table(name = "market_order_items",
        indexes = {
                @Index(name = "idx_seller_order", columnList = "seller_id, order_id"),
                @Index(name = "idx_seller_state", columnList = "seller_id, state")  // 정산 조회용
        }
)
@Getter
@NoArgsConstructor
public class OrderItem extends BaseIdAndTime {
    @ManyToOne(fetch = LAZY)
    private Order order;

    // 정산 조회용
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderItemState state;

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

    public OrderItem(Order order, Long sellerId, Long productId, String productName,
                     String productImageUrl, Long price, Long salePrice,
                     Integer quantity) {
        this.order = order;
        this.sellerId = sellerId;
        this.state = OrderItemState.PENDING_PAYMENT;  // 초기 상태
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


    // 상태 변경 메서드
    public void completePayment() {
        if (this.state != OrderItemState.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }
        this.state = OrderItemState.PAYMENT_COMPLETED;
    }

    public void startPreparing() {
        if (this.state != OrderItemState.PAYMENT_COMPLETED) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }
        this.state = OrderItemState.PREPARING;
    }

    public void startShipping() {
        if (this.state != OrderItemState.PREPARING) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }
        this.state = OrderItemState.SHIPPING;
    }

    public void completeDelivery() {
        if (this.state != OrderItemState.SHIPPING) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }
        this.state = OrderItemState.DELIVERED;
    }

    public void confirm() {
        if (!this.state.isConfirmable()) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }
        this.state = OrderItemState.CONFIRMED;
    }

    public void cancel() {
        if (!this.state.isCancellable()) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        this.state = OrderItemState.CANCELLED;
    }

}
