package com.thock.back.api.boundedContext.finance.reconciliation.domain;

import com.thock.back.api.global.jpa.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "finance_reconciliation_internal_order_snapshot",
        indexes = {
                @Index(name = "idx_snapshot_order_no", columnList = "order_no"), // 주문번호 조회용
                @Index(name = "idx_snapshot_seller", columnList = "seller_id"),    // 판매자별 조회용
                @Index(name = "idx_snapshot_status", columnList = "settlement_status") // 정산 상태별(WAIT/READY) 조회용
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InternalOrderSnapshot extends BaseTimeEntity { // updated_at 포함됨

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문번호 (외부 시스템 ID이므로 String)
    @Column(name = "order_no", nullable = false, length = 255)
    private String orderNo;

    // 판매자 ID
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // 정가 기준 총 판매액 (할인 전)
    @Column(name = "product_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal productAmount;

    // 실제 결제 금액 (최종 정산 대상 금액)
    // 환불일 경우 마이너스가 들어올 수 있음
    @Column(name = "payment_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal paymentAmount;

    // 결제 수단 (CARD, NAVER_PAY 등)
    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // 혹은 별도 Enum 클래스 사용

    // 거래 종류 (PAYMENT: 결제, REFUND: 환불)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    // 메타데이터 (JSON)
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // 주문 스냅샷 찍은 날짜 (주문 발생 시간)
    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;

    // --- 나중에 업데이트되는 필드들 ---

    // 정산 번호 (처음엔 NULL, 정산 확정되면 ID 들어감)
    @Column(name = "monthly_settlement_id")
    private Long monthlySettlementId;

    // 정산 상태 (WAIT -> READY -> COMPLETED)
    @Column(name = "settlement_status", nullable = false, length = 30)
    private String settlementStatus;

    @Builder
    public InternalOrderSnapshot(String orderNo, Long sellerId, BigDecimal productAmount,
                                 BigDecimal paymentAmount, String paymentMethod,
                                 String transactionType, String metadata,
                                 LocalDateTime snapshotAt) {
        this.orderNo = orderNo;
        this.sellerId = sellerId;
        this.productAmount = productAmount;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.transactionType = transactionType;
        this.metadata = metadata;
        this.snapshotAt = snapshotAt;

        // 초기 상태 설정
        this.settlementStatus = "WAIT"; // 기본값: 대기
    }

    // --- 비즈니스 로직 메서드 ---

    // 정산 준비 완료 (대사 끝남)
    public void readySettlement() {
        this.settlementStatus = "READY";
    }

    // 정산 확정 (월별 정산서에 포함됨)
    public void completeSettlement(Long monthlySettlementId) {
        this.monthlySettlementId = monthlySettlementId;
        this.settlementStatus = "COMPLETED";
    }
}