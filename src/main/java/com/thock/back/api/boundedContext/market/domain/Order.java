package com.thock.back.api.boundedContext.market.domain;

import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import com.thock.back.api.shared.market.dto.OrderDto;
import com.thock.back.api.shared.market.event.MarketOrderPaymentCompletedEvent;
import com.thock.back.api.shared.market.event.MarketOrderPaymentRequestedEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "market_orders")
@Getter
@NoArgsConstructor
@Slf4j
public class Order extends BaseIdAndTime {
    @ManyToOne(fetch = LAZY)
    private MarketMember buyer;

    @Column(unique = true, nullable = false, length = 50)
    private String orderNumber;

    @OneToMany(mappedBy = "order", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderState state;

    // 구매자 관점의 금액만
    private Long totalPrice;
    private Long totalSalePrice;
    private Long totalDiscountAmount;

    // 배송지 정보
    @Column(length = 6)
    private String zipCode;
    private String baseAddress;
    private String detailAddress;

    // 결제 관련 시간
    private LocalDateTime requestPaymentDate;  // 결제 요청 시간
    private LocalDateTime paymentDate;         // 결제 완료 시간
    private LocalDateTime cancelDate;          // 취소 시간

    public Order(MarketMember buyer, String zipCode, String baseAddress, String detailAddress) {
        if (buyer == null) {
            throw new CustomException(ErrorCode.CART_USER_NOT_FOUND);
        }

        this.buyer = buyer;
        this.orderNumber = generateOrderNumber();
        this.state = OrderState.PENDING_PAYMENT;
        this.zipCode = zipCode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;

        this.totalPrice = 0L;
        this.totalSalePrice = 0L;
        this.totalDiscountAmount = 0L;
    }

    /**
     * 주문번호 생성: ORDER-20250119-{UUID 8자리}
     */
    private String generateOrderNumber() {
        String date = LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORDER-" + date + "-" + uuid;
    }

    // ProductInfo를 받아서 스냅샷 저장
    public OrderItem addItem(Long sellerId, Long productId, String productName, String productImageUrl,
                             Long price, Long salePrice, Integer quantity) {
        OrderItem orderItem = new OrderItem(this, sellerId, productId, productName, productImageUrl,
                price, salePrice, quantity);

        this.items.add(orderItem);

        this.totalPrice += orderItem.getTotalPrice();
        this.totalSalePrice += orderItem.getTotalSalePrice();
        this.totalDiscountAmount += orderItem.getDiscountAmount();

        return orderItem;
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }

    /**
     * 결제 요청
     * @param balance 사용자 예치금
     * pgAmount : PG로 결제할 금액 (totalSalePrice - balance)
     * pgAmount <= 0: 예치금으로 충분 → MarketOrderPaymentCompletedEvent (pgAmount 없이)
     * pgAmount > 0: PG 결제 필요 → MarketOrderPaymentRequestedEvent (pgAmount 포함)
     */
    public void requestPayment(Long balance) {
        if (this.state != OrderState.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        this.requestPaymentDate = LocalDateTime.now();

        Long pgAmount = Math.max(0L, this.totalSalePrice - balance);

        if (pgAmount <= 0) {
            // 예치금으로 충분 - pgAmount 없이 이벤트 발행
            log.info("💰 예치금 결제: orderId={}, orderNumber={}, totalAmount={}, balance={}",
                    getId(), orderNumber, totalSalePrice, balance);

            publishEvent(new MarketOrderPaymentCompletedEvent(this.toDto()));
        } else {
            // PG 결제 필요 - pgAmount 포함하여 이벤트 발행
            log.info("💳 PG 결제 요청: orderId={}, orderNumber={}, totalAmount={}, pgAmount={}",
                    getId(), orderNumber, totalSalePrice, pgAmount);

            publishEvent(new MarketOrderPaymentRequestedEvent(this.toDto(), pgAmount));
        }
    }

    /**
     * 결제 전 취소
     */
    public void cancelRequestPayment() {
        if (!isPaymentInProgress()) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        this.requestPaymentDate = null;
        this.state = OrderState.CANCELLED;
        this.cancelDate = LocalDateTime.now();

        log.info("❌ 결제 요청 취소: orderId={}, orderNumber={}", getId(), orderNumber);
    }

    /**
     * 결제 완료 처리 (Payment 모듈이 호출)
     * TODO : Payment 모듈이 결제 완료 후 이 메서드를 호출함 (이벤트 리스너를 통해)
     */
    public void completePayment() {
        if (this.state != OrderState.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        this.state = OrderState.PAYMENT_COMPLETED;
        this.paymentDate = LocalDateTime.now();

        // 모든 OrderItem도 결제 완료 상태로 변경
        this.items.forEach(OrderItem::completePayment);

        log.info("✅ 결제 완료: orderId={}, orderNumber={}, paymentDate={}",
                getId(), orderNumber, paymentDate);
    }

    /**
     * 주문 전체 취소
     */
    public void cancel() {
        if (!this.state.isCancellable()) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        OrderState previousState = this.state;
        boolean needsRefund = previousState == OrderState.PAYMENT_COMPLETED ||
                previousState == OrderState.PREPARING;

        // 모든 OrderItem 취소
        this.items.forEach(OrderItem::cancel);

        this.state = OrderState.CANCELLED;
        this.cancelDate = LocalDateTime.now();

        log.info("🚫 주문 전체 취소: orderId={}, orderNumber={}, previousState={}, cancelDate={}",
                getId(), orderNumber, previousState, cancelDate);

        if (needsRefund) {
            log.info("💸 환불 필요: orderId={}, refundAmount={}", getId(), totalSalePrice);
            // TODO: MarketOrderRefundRequestedEvent 발행
        }
    }

    /**
     * 특정 상품만 취소 (부분 취소)
     */
    public void cancelItem(Long orderItemId) {
        OrderItem orderItem = items.stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        if (!orderItem.getState().isCancellable()) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        orderItem.cancel();
        updateStateFromItems();

        log.info("🚫 상품 부분 취소: orderId={}, orderItemId={}, productName={}",
                getId(), orderItemId, orderItem.getProductName());
    }

    /**
     * Order 전체 상태를 OrderItem 상태 기반으로 계산
     */
    public void updateStateFromItems() {
        if (items.isEmpty()) {
            return;
        }

        long confirmedCount = items.stream()
                .filter(item -> item.getState() == OrderItemState.CONFIRMED)
                .count();

        long cancelledCount = items.stream()
                .filter(item -> item.getState() == OrderItemState.CANCELLED)
                .count();

        long shippingCount = items.stream()
                .filter(item -> item.getState() == OrderItemState.SHIPPING)
                .count();

        long deliveredCount = items.stream()
                .filter(item -> item.getState() == OrderItemState.DELIVERED)
                .count();

        int totalItems = items.size();

        if (confirmedCount == totalItems) {
            this.state = OrderState.CONFIRMED;
        } else if (cancelledCount == totalItems) {
            this.state = OrderState.CANCELLED;
        } else if (cancelledCount > 0) {
            this.state = OrderState.PARTIALLY_CANCELLED;
        } else if (deliveredCount == totalItems) {
            this.state = OrderState.DELIVERED;
        } else if (shippingCount > 0) {
            this.state = shippingCount == totalItems ?
                    OrderState.SHIPPING : OrderState.PARTIALLY_SHIPPED;
        } else if (this.state == OrderState.PAYMENT_COMPLETED) {
            this.state = OrderState.PREPARING;
        }
    }

    public boolean isPaymentInProgress() {
        return requestPaymentDate != null &&
                paymentDate == null &&
                cancelDate == null;
    }

    public boolean isPaid() {
        return this.paymentDate != null;
    }

    public OrderDto toDto() {
        return new OrderDto(
                getId(),
                buyer.getId(),
                buyer.getName(),
                getOrderNumber(),
                getTotalSalePrice()
        );
    }
}