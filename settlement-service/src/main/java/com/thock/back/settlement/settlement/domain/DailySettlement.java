package com.thock.back.settlement.settlement.domain;

import com.github.f4b6a3.tsid.TsidCreator;
import com.thock.back.settlement.settlement.domain.enums.DailySettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "daily_settlement")
public class DailySettlement {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long sellerId; // 판매자 ID

    @Column(nullable = false)
    private LocalDate targetDate; // 정산 기준일 (언제 발생한 매출인가)

    @Column(nullable = false)
    private Long paymentAmount; // 결제 원금 (상계 처리된 아이템들의 총합)

    @Column(nullable = false)
    private Long feeAmount; // 차감 수수료 (예: 원금 * 0.2)

    @Column(nullable = false)
    private Long settlementAmount; // 정산 확정 금액 (실 지급액)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DailySettlementStatus status; // PENDING, COMPLETED 등

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 자식 테이블이 부모의 생명주기와 함께하므로 OneToMany, 연관관계 편의 메서드까지 작성
    @Builder.Default
    @OneToMany(mappedBy = "dailySettlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailySettlementItem> items = new ArrayList<>();

    //  부모가 참조하는 자식 객체가 추가 될 때, 자식의 부모에도 바로 연결 해주는 연관관계 편의메서드
    public void addItem(DailySettlementItem item) {
        this.items.add(item);
        item.assignSettlement(this);
    }

    // ----------- 비즈니스메소드 ----------
    public void calculateTotalAmount(BigDecimal feeRate){
        long totalAmounts = 0L;
        // 해당 정산서의 총 판매액을 계산
        for(DailySettlementItem item : this.items){
            totalAmounts += item.getFinalAmount();
        }

        // 수수료 계산
        BigDecimal feeDecimal = BigDecimal.valueOf(totalAmounts).multiply(feeRate);
        long fee = feeDecimal.longValue();

        // 가정산 금액 계산
        long dailyPayout = totalAmounts - fee;

        // 이 객체 데이터 업데이트
        this.updateAmounts(totalAmounts, fee, dailyPayout);
    }



    // -------------- 생성자 --------------

    @Builder
    public DailySettlement(Long sellerId, LocalDate targetDate, Long paymentAmount, Long feeAmount, Long settlementAmount) {
        this.sellerId = sellerId;
        this.targetDate = targetDate;
        this.paymentAmount = paymentAmount;
        this.feeAmount = feeAmount;
        this.settlementAmount = settlementAmount;
        this.status = DailySettlementStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    public static DailySettlement create(Long sellerId, LocalDate targetDate) {
        return DailySettlement.builder()
                .sellerId(sellerId)
                .targetDate(targetDate)
                .paymentAmount(0L)
                .feeAmount(0L)
                .settlementAmount(0L)
                .status(DailySettlementStatus.PENDING)
                .build();
    }


    public void updateAmounts(Long paymentAmount, Long feeAmount, Long settlementAmount){
        this.paymentAmount = paymentAmount;
        this.feeAmount = feeAmount;
        this.status = DailySettlementStatus.COMPLETED;
        this.settlementAmount = settlementAmount;
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            // 저장하기 전에 TSID를 생성해서 넣음
            this.id = TsidCreator.getTsid().toLong();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
