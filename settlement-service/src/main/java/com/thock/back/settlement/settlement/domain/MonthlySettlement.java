package com.thock.back.settlement.settlement.domain;

import com.github.f4b6a3.tsid.TsidCreator;
import com.thock.back.settlement.shared.money.Money;
import com.thock.back.settlement.shared.money.MoneyAttributeConverter;
import com.thock.back.settlement.settlement.domain.enums.MonthlySettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "monthly_settlement")
@EntityListeners(AuditingEntityListener.class) // ★ created_at 자동 주입
public class MonthlySettlement {

    @Id
    private Long id; // TSID

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "target_year_month", nullable = false)
    private String targetYearMonth; // "202602"


    @Column(name = "total_count")
    private Long totalCount;

    @Column(name = "total_payment_amount")
    @Convert(converter = MoneyAttributeConverter.class)
    private Money totalPaymentAmount;

    @Column(name = "total_fee_amount")
    @Convert(converter = MoneyAttributeConverter.class)
    private Money totalFeeAmount;

    @Column(name = "total_payout_amount")
    @Convert(converter = MoneyAttributeConverter.class)
    private Money totalPayoutAmount;

    // 상태 및 운영 관련 컬럼
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MonthlySettlementStatus status; // PENDING -> PROCESSING -> COMPLETED / FAILED

    @Column(name = "retry_count")
    private int retryCount = 0; // 재시도 횟수 (기본값 0)

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // 실패 사유

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 집계 생성 시간

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 포인트 지급 완료 시간

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = TsidCreator.getTsid().toLong();
        }
    }

    @Builder
    public MonthlySettlement(Long sellerId, String targetYearMonth, Long totalCount,
                             Money totalPaymentAmount, Money totalFeeAmount, Money totalPayoutAmount) {
        this.sellerId = sellerId;
        this.targetYearMonth = targetYearMonth;
        this.totalCount = totalCount;
        this.totalPaymentAmount = totalPaymentAmount;
        this.totalFeeAmount = totalFeeAmount;
        this.totalPayoutAmount = totalPayoutAmount;
        this.status = MonthlySettlementStatus.PENDING;
    }

    // --- [비즈니스 메서드] ---

    // 1. 지급 시작 (처리 중)
    public void startPayout() {
        this.status = MonthlySettlementStatus.PROCESSING;
    }

    // 2. 지급 완료 (성공)
    public void completePayout() {
        this.status = MonthlySettlementStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = null; // 성공했으니 에러 메시지 삭제
    }

    // 3. 지급 실패 (재시도 로직 포함)
    public void failPayout(String message) {
        this.retryCount++; // 횟수 증가
        this.errorMessage = message;

        // 재시도 횟수가 3번 넘어가면 완전히 실패 처리
        if (this.retryCount >= 3) {
            this.status = MonthlySettlementStatus.FAILED;
        } else {
            // 아직 기회가 남았으면 상태는 유지하거나 RETRY_WAIT 등으로 변경 가능
            // 여기서는 일단 PROCESSING 유지 혹은 PENDING으로 되돌리기 전략 선택
            this.status = MonthlySettlementStatus.PENDING; // 다음 배치 때 다시 시도하게 둠
        }
    }
}
