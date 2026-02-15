package com.thock.back.market.domain;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.jpa.entity.BaseIdAndTime;
import com.thock.back.shared.market.domain.CancelReasonType;
import com.thock.back.shared.market.dto.OrderDto;
import com.thock.back.shared.market.event.MarketOrderBeforePaymentCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentCompletedEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestCanceledEvent;
import com.thock.back.shared.market.event.MarketOrderPaymentRequestedEvent;
import com.thock.back.shared.market.event.MarketOrderSettlementEvent;
import com.thock.back.shared.payment.dto.BeforePaymentCancelRequestDto;
import com.thock.back.shared.payment.dto.PaymentCancelRequestDto;
import com.thock.back.shared.settlement.dto.SettlementOrderItemDto;
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
     * 주문번호 생성: ORDER-20250119-{UUID 12자리}
     */
    private String generateOrderNumber() {
        String date = LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
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
     * 결제 완료 처리 (Payment 모듈이 호출)
     */
    public void completePayment() {
        // 이미 결제 완료면 조용히 무시 (멱등성 보장)
        if (this.state == OrderState.PAYMENT_COMPLETED) {
            log.info("이미 결제 완료된 주문 (중복 이벤트 무시): orderNumber={}", orderNumber);
            return;
        }


        if (this.state != OrderState.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        this.state = OrderState.PAYMENT_COMPLETED;
        this.paymentDate = LocalDateTime.now();

        // 모든 OrderItem도 결제 완료 상태로 변경
        this.items.forEach(OrderItem::completePayment);

        log.info("✅ 결제 완료: orderId={}, orderNumber={}, paymentDate={}",
                getId(), orderNumber, paymentDate);

        // Settlement 이벤트 발행 (결제 완료)
        publishSettlementEvent(SettlementEventType.PAYMENT_COMPLETED);
    }

    /**
     * 주문 전체 취소 1. 결제 요청 중 취소
     * PG 결제창 띄워놓고 사용자가 취소(명시적 취소)하거나 결제 안 하고(타임 아웃) 나간 경우
     */
    public void cancelRequestPayment(CancelReasonType cancelReasonType, String cancelReasonDetail) {
        if (!isPaymentInProgress()) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        // 모든 OrderItem 취소
        this.items.forEach(item -> item.cancel(cancelReasonType, cancelReasonDetail));

        this.state = OrderState.CANCELLED;
        this.cancelDate = LocalDateTime.now();

        log.info("❌ 결제 요청 취소: orderId={}, orderNumber={}, reason={}", getId(), orderNumber, cancelReasonType);

        // Payment 모듈에 결제 전 취소 알림 (REQUESTED → CANCELED)
        BeforePaymentCancelRequestDto cancelDto = new BeforePaymentCancelRequestDto(this.orderNumber);
        publishEvent(new MarketOrderBeforePaymentCanceledEvent(cancelDto));
    }

    /**
     * 주문 전체 취소 2. 결제까지 완료 한 경우
     */
    public void cancel(CancelReasonType cancelReasonType, String cancelReasonDetail) {
        if (!this.state.isCancellable()) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        OrderState previousState = this.state;

        // 모든 OrderItem 취소
        this.items.forEach(item -> item.cancel(cancelReasonType, cancelReasonDetail));

        this.state = OrderState.CANCELLED;
        this.cancelDate = LocalDateTime.now();

        log.info("🚫 주문 전체 취소: orderId={}, orderNumber={}, previousState={}, reason={}",
                getId(), orderNumber, previousState, cancelReasonType);

        // 결제 완료된 주문만 환불 필요 (PENDING_PAYMENT 제외)
        if (isPaid()) {
            log.info("💸 환불 필요: orderId={}, refundAmount={}", getId(), totalSalePrice);

            String cancelReason = cancelReasonType == CancelReasonType.ETC && cancelReasonDetail != null
                    ? String.format("%s: %s", cancelReasonType.getDescription(), cancelReasonDetail)
                    : cancelReasonType.getDescription();

            PaymentCancelRequestDto cancelDto = new PaymentCancelRequestDto(
                    this.orderNumber,
                    String.format("주문 전체 취소 (사유: %s)", cancelReason),
                    this.totalSalePrice  // 전액 환불
            );
            publishEvent(new MarketOrderPaymentRequestCanceledEvent(cancelDto));
        }
    }

    /**
     * 부분 취소
     */
    public void cancelItems(List<Long> orderItemIds, CancelReasonType cancelReasonType, String cancelReasonDetail) {
        // 1. 취소할 아이템들 조회 및 검증
        // TODO : id가 unique이긴 하지만 findFirst가 뭔가 조금 어색함
        List<OrderItem> orderItems = orderItemIds.stream()
                .map(id -> items.stream()
                        .filter(item -> item.getId().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new CustomException(ErrorCode.ORDER_ITEM_NOT_FOUND)))
                .toList();

        // 2. 취소 가능 상태 확인
        orderItems.forEach(item -> {
            if (!item.getState().isCancellable()) {
                throw new CustomException(ErrorCode.ORDER_CANNOT_CANCEL);
            }
        });

        // 3. 부분 환불 금액 계산
        Long refundAmount = orderItems.stream()
                .mapToLong(OrderItem::getTotalSalePrice)
                .sum();

        // 4. 각 아이템 취소 처리
        orderItems.forEach(item -> item.cancel(cancelReasonType, cancelReasonDetail));
        updateStateFromItems();

        log.info("🚫 상품 부분 취소: orderId={}, count={}, reason={}",
                getId(), orderItemIds.size(), cancelReasonType);

        // 5. 결제 완료 후에만 환불 이벤트 (한 번만 발행)
        if (this.isPaid()) {
            // ETC인 경우에만 사용자가 취소 사유 직접 입력 가능(선택)
            String cancelReason = cancelReasonType == CancelReasonType.ETC && cancelReasonDetail != null
                    ? String.format("%s: %s", cancelReasonType.getDescription(), cancelReasonDetail)
                    : cancelReasonType.getDescription();

            PaymentCancelRequestDto cancelDto = new PaymentCancelRequestDto(
                    this.orderNumber,
                    String.format("주문 상품 부분 취소 (%d개, 사유: %s)", orderItems.size(), cancelReason),
                    refundAmount
            );
            publishEvent(new MarketOrderPaymentRequestCanceledEvent(cancelDto));

            log.info("💸 부분 환불 요청: orderId={}, refundAmount={}", getId(), refundAmount);
        }
    }

    /**
     * 환불 완료 처리 (Payment 모듈에서 환불 완료 이벤트 수신 시)
     * 부분 환불 시 나머지 아이템들은 강제 구매확정 처리
     * 이후 추가 취소/환불은 CS 처리로 진행
     * TODO: 환불 주문 건 정산으로 넘기기, 각 orderItem에 대해서 수량 별 취소까지 가능
     */
    public void completeRefund(){
        // 이미 부분, 전체 환불이면 무시 (멱등성)
        if (this.state == OrderState.PARTIALLY_REFUNDED || this.state == OrderState.REFUNDED) {
            log.info("이미 환불 완료된 주문 (중복 이벤트 무시): orderNumber={}", orderNumber);
            return;
        }

        // 주문 취소 이후 상태 : 환불 가능한 상태 체크
        if (!this.state.canCompleteRefund()) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_REFUND);
        }

        // 1. 취소된 아이템들을 환불 완료로 변경
        this.items.stream()
                .filter(item -> item.getState().canCompleteRefund())
                .forEach(OrderItem::completeRefund);

        // 2. 활성 상태 아이템들을 강제 구매확정 처리
        List<OrderItem> forceConfirmedItems = this.items.stream()
                .filter(item -> item.getState().isActiveState())
                .toList();
        forceConfirmedItems.forEach(OrderItem::forceConfirm);

        // 3. 상태 결정: 모든 아이템이 REFUNDED인지 확인
        boolean allRefunded = this.items.stream()
                .allMatch(item -> item.getState() == OrderItemState.REFUNDED);

        // 4. Settlement 이벤트 발행: 환불 아이템 + 강제 구매확정 아이템
        List<SettlementOrderItemDto> refundedItems = this.items.stream()
                .filter(item -> item.getState() == OrderItemState.REFUNDED)
                .map(item -> item.toSettlementDto(SettlementEventType.REFUND_COMPLETED))
                .toList();

        List<SettlementOrderItemDto> confirmedItems = forceConfirmedItems.stream()
                .map(item -> item.toSettlementDto(SettlementEventType.PURCHASE_CONFIRMED))
                .toList();

        if (!refundedItems.isEmpty()) {
            publishEvent(new MarketOrderSettlementEvent(refundedItems));
            log.info("📊 환불 Settlement 이벤트 발행: orderNumber={}, count={}", orderNumber, refundedItems.size());
        }

        if (!confirmedItems.isEmpty()) {
            publishEvent(new MarketOrderSettlementEvent(confirmedItems));
            log.info("📊 환불 Settlement 이벤트 발행: orderNumber={}, count={}", orderNumber, confirmedItems.size());
        }


        if (allRefunded) {
            this.state = OrderState.REFUNDED;
            log.info("💰 전체 환불 완료: orderId={}, orderNumber={}", getId(), getOrderNumber());
        } else {
            this.state = OrderState.PARTIALLY_REFUNDED;
            log.info("💰 부분 환불 완료: orderId={}, orderNumber={}", getId(), getOrderNumber());
        }
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

    /**
     * 주문 전체 구매 확정
     */
    public void confirm() {
        if (!this.state.isConfirmable()) {
            throw new CustomException(ErrorCode.ORDER_INVALID_STATE);
        }

        // 모든 OrderItem 구매 확정
        this.items.forEach(OrderItem::confirm);
        this.state = OrderState.CONFIRMED;

        log.info("✅ 구매 확정: orderId={}, orderNumber={}", getId(), orderNumber);

        // Settlement 이벤트 발행 (구매 확정)
        publishSettlementEvent(SettlementEventType.PURCHASE_CONFIRMED);
    }

    /**
     * Settlement 정산 이벤트 발행 헬퍼
     */
    private void publishSettlementEvent(SettlementEventType eventType) {
        List<SettlementOrderItemDto> settlementItems = this.items.stream()
                .map(item -> item.toSettlementDto(eventType))
                .toList();

        publishEvent(new MarketOrderSettlementEvent(settlementItems));
        log.info("📊 Settlement 이벤트 발행: orderNumber={}, eventType={}, itemCount={}",
                orderNumber, eventType, settlementItems.size());
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