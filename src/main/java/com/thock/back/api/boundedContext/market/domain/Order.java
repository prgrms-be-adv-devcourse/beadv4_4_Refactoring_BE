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
    private String orderNumber; // 주문번호 (예: ORDER-20250119-UUID)

    @OneToMany(mappedBy = "order", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderState state;

    // 가격 정보
    private Long totalPrice;           // 총 정가
    private Long totalSalePrice;       // 총 판매가
    private Long totalDiscountAmount;  // 총 할인액
    private Long totalPayoutAmount;    // 총 정산 금액 (판매자가 받을 금액)
    private Long totalFeeAmount;       // 총 수수료 (플랫폼 수익)

    // 배송지 정보 (주문 시점 스냅샷)
    @Column(length = 6)
    private String zipCode;
    private String baseAddress;
    private String detailAddress;

    // 결제 관련 시간
    private LocalDateTime requestPaymentDate;  // 결제 요청 시간
    private LocalDateTime paymentDate;         // 결제 완료 시간
    private LocalDateTime cancelDate;          // 취소 시간

    // Cart로부터 Order생성
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

        // 가격 정보 초기화
        this.totalPrice = 0L;
        this.totalSalePrice = 0L;
        this.totalDiscountAmount = 0L;
        this.totalPayoutAmount = 0L;
        this.totalFeeAmount = 0L;
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
    public OrderItem addItem(Long productId, String productName, String productImageUrl,
                             Long price, Long salePrice, Integer quantity) {
        OrderItem orderItem = new OrderItem(this, productId, productName, productImageUrl,
                price, salePrice, quantity);

        this.items.add(orderItem);

        // 주문 총액 업데이트
        this.totalPrice += orderItem.getTotalPrice();
        this.totalSalePrice += orderItem.getTotalSalePrice();
        this.totalDiscountAmount += orderItem.getDiscountAmount();
        this.totalPayoutAmount += orderItem.getPayoutAmount();
        this.totalFeeAmount += orderItem.getFeeAmount();

        return orderItem;
    }

    public boolean hasItems(){
        return !items.isEmpty();
    }

    /**
     * 결제 요청
     * TODO : pgPaymentAmount Order에서 예치금 확인 및 계산 전부 된 값 보내주어야함.
     */
    public void requestPayment(Long pgPaymentAmount){
        if (this.state != OrderState.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        this.requestPaymentDate = LocalDateTime.now();
        log.info("💳 결제 요청: orderId={}, orderNumber={}, amount={}",
                getId(), orderNumber, pgPaymentAmount);

        // 이벤트 발생
        publishEvent(new MarketOrderPaymentRequestedEvent(
                this.toDto(),
                pgPaymentAmount
        ));
    }

    /**
     * 결제 요청 취소 - 결제 요청 중인 상태만 취소 가능
     */
    public void cancelRequestPayment() {
        if (!isPaymentInProgress()){
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }
        this.requestPaymentDate = null;
        log.info("❌ 결제 요청 취소: orderId={}, orderNumber={}", getId(), orderNumber);
    }

    /**
     * 결제 완료 처리
     */
    public void completePayment() {
        if (this.state != OrderState.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        this.state = OrderState.PAYMENT_COMPLETED;
        this.paymentDate = LocalDateTime.now();

        log.info("✅ 결제 완료: orderId={}, orderNumber={}, paymentDate={}",
                getId(), orderNumber, paymentDate);

        // 이벤트 발생
        publishEvent(new MarketOrderPaymentCompletedEvent(
                this.toDto()
        ));
    }

    /**
     * 주문 취소
     */
    public void cancel() {
        if (!this.state.isCancellable()) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        this.state = OrderState.CANCELLED;
        this.cancelDate = LocalDateTime.now();

        log.info("🚫 주문 취소: orderId={}, orderNumber={}, cancelDate={}",
                getId(), orderNumber, cancelDate);
    }

    /**
     * 배송 준비 시작
     */
    public void startPreparing() {
        if (this.state != OrderState.PAYMENT_COMPLETED) {
             throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        this.state = OrderState.PREPARING;
    }

    /**
     * 배송 시작
     */
    public void startShipping() {
        if (this.state != OrderState.PREPARING) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        this.state = OrderState.SHIPPING;
    }

    /**
     * 배송 완료
     */
    public void completeDelivery() {
        if (this.state != OrderState.SHIPPING) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        this.state = OrderState.DELIVERED;
    }

    /**
     * 구매 확정
     * 이것만 메서드 사용하는 이유?
     * if(!this.state = OrderState.DELIVERED) 하면 되잖아
     */
    public void confirm() {
        if (!this.state.isConfirmable()) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        this.state = OrderState.CONFIRMED;
    }

    // ========== 상태 체크 메서드 ==========
    public boolean isPaid() {
        return paymentDate != null;
    }

    public boolean isCanceled() {
        return cancelDate != null;
    }

    public boolean isPaymentInProgress() {
        return requestPaymentDate != null &&
                paymentDate == null &&
                cancelDate == null;
    }

    // Dto
    public OrderDto toDto(){
        return new OrderDto(
                getId(),
                buyer.getId(),
                buyer.getName(),
                getOrderNumber(),
                getState().name(),
                getTotalPrice(),
                getTotalSalePrice(),
                getRequestPaymentDate(),
                getPaymentDate()
        );
    }

}
