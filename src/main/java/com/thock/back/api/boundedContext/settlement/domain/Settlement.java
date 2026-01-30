package com.thock.back.api.boundedContext.settlement.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "settlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SettlementMember와 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id") // 컬럼명도 member_id로 변경 추천
    private SettlementMember member;

    @Column(name = "sales_date")
    private LocalDate salesDate;

    @Column(name = "total_amount")
    private Long totalAmount;   // 총 판매금액 (할인 후 실 결제액 합계)

    @Column(name = "payout_amount")
    private Long payoutAmount;  // 판매자 정산금액 (수수료 제외 후)

    @Column(name = "fee_amount")
    private Long feeAmount;     // 수수료 합계

    @Column(name = "applied_fee_rate")
    private Double appliedFeeRate; // OrderItem.payoutRate (Double)

    // ----------------------------------------

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;

    private Integer retryCount;
    private String failureReason;
    private LocalDateTime processedAt;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementDetail> details = new ArrayList<>();

    @Builder
    public Settlement(SettlementMember member, LocalDate salesDate, Long totalAmount,
                      Long payoutAmount, Long feeAmount, Double appliedFeeRate) {
        this.member = member;
        this.salesDate = salesDate;
        this.totalAmount = totalAmount;
        this.payoutAmount = payoutAmount;
        this.feeAmount = feeAmount;
        this.appliedFeeRate = appliedFeeRate;
        this.status = SettlementStatus.PENDING;
        this.retryCount = 0;
    }

    public void addDetail(SettlementDetail detail) {
        this.details.add(detail);
        detail.setSettlement(this);
    }

    public void complete(LocalDateTime processedAt) {
        this.status = SettlementStatus.COMPLETED;
        this.processedAt = processedAt;
    }

    public enum SettlementStatus { PENDING, COMPLETED, FAILED }
}