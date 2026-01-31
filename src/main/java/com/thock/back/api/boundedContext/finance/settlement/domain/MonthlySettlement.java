package com.thock.back.api.boundedContext.finance.settlement.domain;

import com.thock.back.api.boundedContext.finance.global.jpa.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "finance_settlement_monthly_settlement", // 테이블명 명시
        indexes = {
                @Index(name = "idx_monthly_seller", columnList = "seller_id"),   // 판매자별 조회 (필수)
                @Index(name = "idx_monthly_status", columnList = "status"),      // "PENDING" 상태인 것만 배치로 뽑아야 함 (필수)
                @Index(name = "idx_monthly_target", columnList = "target_date")  // "2026-01" 정산 내역 조회용
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlySettlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 판매자 ID (누구한테 줄 돈이냐)
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // 정산 기준 월 (이미지에서 VARCHAR(30)이라 하셨으므로 String)
    // 값 예시: "2026-01" 또는 "2026-01-01~2026-01-31"
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    // 지급 총액 (매출 합계)
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount;

    // 수수료 총액 (플랫폼 수수료)
    @Column(name = "fee_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal feeAmount;

    // [실 지급액] = 총액 - 수수료 (실제 이체할 금액)
    @Column(name = "payout_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal payoutAmount;

    // 정산 상태 (PENDING, PROCESSING, COMPLETED)
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    // 계좌정보 스냅샷
    // 정산이 확정된 시점의 계좌 정보를 박제해놔야 함.
    // (정산금 지급 직전에 판매자가 계좌를 바꾸더라도, 정산 시점의 계좌로 나가야 안전함)

    @Column(name = "bank_code_snapshot", length = 50)
    private String bankCodeSnapshot;

    @Column(name = "account_number_snapshot", length = 50)
    private String accountNumberSnapshot;

    @Column(name = "account_holder_snapshot", length = 50)
    private String accountHolderSnapshot;

    // 정산 끝나고 이체 완료 시각
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // --- 시간 정보 ---

    // 집계 완료 및 생성 시간 createdAt 상속받아 사용

    // 상태 변경 시간 updatedAt 상속받아 사용


    @Builder
    public MonthlySettlement(Long sellerId, LocalDate targetDate, BigDecimal totalAmount, BigDecimal feeAmount,
                             String bankCodeSnapshot, String accountNumberSnapshot, String accountHolderSnapshot) {
        this.sellerId = sellerId;
        this.targetDate = targetDate;
        this.totalAmount = totalAmount;
        this.feeAmount = feeAmount;
        // 실 지급액은 생성 시점에 자동 계산 (안전장치)
        this.payoutAmount = totalAmount.subtract(feeAmount);
        this.status = "PENDING"; // 초기 상태는 무조건 PENDING
        this.bankCodeSnapshot = bankCodeSnapshot;
        this.accountNumberSnapshot = accountNumberSnapshot;
        this.accountHolderSnapshot = accountHolderSnapshot;
    }

    // --- 비즈니스 로직 메서드 ---

    //Payout -> payment 모듈에 정산이 끝남을 알리는 행위(이때 전달해주는 데이터로 wallet의 포인트가 올라감)

    // 지급 진행 중 (배치 진행중)
    public void startPayout() {
        this.status = "PROCESSING";
    }

    // 배치 끝나고, payment 모듈에 알림 후 포인트 지급 완료
    public void successPayout() {
        this.status = "COMPLETED";
    }
    // 실패 건들은 PENDING으로 남겨 재배치
    public void failPayout() {
        this.status = "PENDING";
    }

}