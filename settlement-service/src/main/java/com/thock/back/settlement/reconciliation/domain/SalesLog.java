package com.thock.back.settlement.reconciliation.domain;

import com.github.f4b6a3.tsid.TsidCreator;
import com.thock.back.global.jpa.entity.BaseTimeEntity;
import com.thock.back.settlement.reconciliation.domain.enums.PaymentMethod;
import com.thock.back.settlement.reconciliation.domain.enums.ReconciliationStatus;
import com.thock.back.settlement.shared.converter.MapToJsonConverter;
import com.thock.back.settlement.shared.enums.TransactionType;
import com.thock.back.settlement.shared.money.Money;
import com.thock.back.settlement.shared.money.MoneyAttributeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
        name = "sales_log",
        indexes = {
                @Index(name = "idx_sales_log_order_no", columnList = "order_no"), // 주문번호 조회용
                @Index(name = "idx_sales_log_seller", columnList = "seller_id"),    // 판매자별 조회용
                @Index(name = "idx_sales_log_status", columnList = "reconciliation_status") // 정산 상태별(WAIT/READY) 조회용
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesLog extends BaseTimeEntity { // updated_at 포함됨

    @Id
    private Long id; // TSID

    // 주문번호 (외부 시스템 ID이므로 String)
    @Column(name = "order_no", nullable = false, length = 255)
    private String orderNo;

    // 판매자 ID
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_quantity", nullable = false)
    private int productQuantity;

    // 정가 기준 총 판매액 (할인 전)
    @Column(name = "product_price", nullable = false)
    @Convert(converter = MoneyAttributeConverter.class)
    private Money productPrice;

    // 실제 결제 금액 (최종 정산 대상 금액)
    // 환불일 경우 마이너스가 들어올 수 있음
    @Column(name = "payment_amount", nullable = false)
    @Convert(converter = MoneyAttributeConverter.class)
    private Money paymentAmount;

    // 결제 수단 (CARD, NAVER_PAY 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod; // 혹은 별도 Enum 클래스 사용

    // 거래 종류 (PAYMENT: 결제,구매확정, REFUND: 환불)
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    // 메타데이터 (JSON)
    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "metadata", columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.PENDING;


    // 주문 스냅샷 찍은 날짜 (주문 발생 시간)
    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;


    // 일별 정산 진행 후 업데이트 됨
    // 정산 번호 (처음엔 NULL, 정산 확정되면 ID 들어감)
    @Column(name = "daily_settlement_id")
    private Long dailySettlementId;

    // 생성자 필드
    @Builder
    public SalesLog(String orderNo, Long sellerId, Long productId, Money productPrice,
                    Money paymentAmount, PaymentMethod paymentMethod,
                    TransactionType transactionType, Map<String, Object> metadata,
                    LocalDateTime snapshotAt) {
        this.orderNo = orderNo;
        this.sellerId = sellerId;
        this.productId = productId;
        this.productPrice = productPrice;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.transactionType = transactionType;
        this.metadata = metadata;
        this.snapshotAt = snapshotAt;
        // 초기 상태 설정
        this.reconciliationStatus = ReconciliationStatus.PENDING;
    }

    // --- 비즈니스 로직 메소드 ---

    // 일별 정산의 ID를 삽입해서 일별 정산이 진행되었음을 확인할 수 있도록
    public void markAsSettled(Long dailySettlementId){
        this.dailySettlementId = dailySettlementId;
    }

    // 대사 관련 필드 메소드

    public void matchReconciliation() {
        this.reconciliationStatus = ReconciliationStatus.MATCH;
    }
    public void mismatchReconciliation(){
        this.reconciliationStatus = ReconciliationStatus.MISMATCH;
    }
    public void confirm(){
        this.confirmedAt = LocalDateTime.now();
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            // 저장하기 전에 TSID를 생성해서 넣음
            this.id = TsidCreator.getTsid().toLong();
        }
    }
}
