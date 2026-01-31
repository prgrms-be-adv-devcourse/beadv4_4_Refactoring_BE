package com.thock.back.api.boundedContext.finance.settlement.domain;
import com.thock.back.api.boundedContext.finance.global.jpa.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_withdrawal_request") // 출금 요청 테이블
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId; // 누가 요청했냐

    @Column(name = "withdrawal_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal withdrawalAmount; // 얼마 뺄 거냐

    // --- 돈 보낼 곳 (신청 시점의 계좌 정보 스냅샷) ---
    // 나중에 계좌 바꿔도, 신청 당시 계좌로 기록이 남아야 함
    @Column(name = "bank_code_snapshot", nullable = false)
    private String bankCodeSnapshot;

    @Column(name = "account_number_snapshot", nullable = false)
    private String accountNumberSnapshot;

    @Column(name = "account_holder_snapshot", nullable = false)
    private String accountHolderSnapshot;

    // --- 상태 관리 ---
    @Column(name = "status", nullable = false)
    private String status; // REQUESTED(요청됨) -> PROCESSING -> COMPLETED or FAILED

    @Column(name = "processed_at")
    private LocalDateTime processedAt; // 이체 완료 시간

    @Builder
    public WithdrawalRequest(Long sellerId, BigDecimal withdrawalAmount, SettlementAccount account) {
        this.sellerId = sellerId;
        this.withdrawalAmount = withdrawalAmount;
        // 신청 시점의 계좌 정보를 복사해서 박제(Snapshot)
        this.bankCodeSnapshot = account.getBankCode();
        this.accountNumberSnapshot = account.getAccountNumber();
        this.accountHolderSnapshot = account.getAccountHolder();
        this.status = "REQUESTED";
    }

    // 비즈니스 로직

    public void startTransfer(){
        this.status = "PROCESSING";

    }

    public void failTransfer(){
        this.status = "FAILED";
        this.processedAt = LocalDateTime.now();
    }

    public void completeTransfer() {
        this.status = "COMPLETED";
        this.processedAt = LocalDateTime.now();
    }
}