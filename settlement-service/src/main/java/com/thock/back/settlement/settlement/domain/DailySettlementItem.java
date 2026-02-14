package com.thock.back.settlement.settlement.domain;

import com.github.f4b6a3.tsid.TsidCreator;
import com.thock.back.settlement.reconciliation.app.port.SettlementCandidate;
import com.thock.back.settlement.shared.money.Money;
import com.thock.back.settlement.shared.money.MoneyAttributeConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "daily_settlement_item")
public class DailySettlementItem {

    @Id
    private Long id;

    // FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_settlement_id", nullable = false)
    private DailySettlement dailySettlement;

    @Column(nullable = false)
    private Long productId; // 상품 ID

    @Column(nullable = false)
    private String productName; // 상품명

    @Column(nullable = false)
    private int finalQuantity; // 상계 처리 후 최종 수량 (+, - 가능)

    @Column(nullable = false)
    @Convert(converter = MoneyAttributeConverter.class)
    private Money finalAmount; // 상계 처리 후 최종 금액 (+, - 가능)

    @Builder
    public DailySettlementItem(Long productId, String productName, int finalQuantity, Money finalAmount) {
        this.productId = productId;
        this.productName = productName;
        this.finalQuantity = finalQuantity;
        this.finalAmount = finalAmount;
    }

    // 연관관계 편의 메서드
    public void assignSettlement(DailySettlement dailySettlement) {
        this.dailySettlement = dailySettlement;
    }

    // -----------생성자----------

    public static DailySettlementItem from(List<SettlementCandidate> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("집계할 데이터가 없습니다.");
        }

        // 1. 합계 계산 (서비스에서 하던 거)
        int totalQuantity = candidates.stream().mapToInt(SettlementCandidate::productQuantity).sum();
        long totalAmount = candidates.stream().mapToLong(SettlementCandidate::paymentAmount).sum();

        // 2. 이름 꺼내기
        String productName = candidates.get(0).productName();
        Long productId = candidates.get(0).productId();

        // 3. 내 자신(아이템) 생성해서 반환
        return DailySettlementItem.builder()
                .productId(productId)
                .productName(productName)
                .finalQuantity(totalQuantity)
                .finalAmount(Money.of(totalAmount))
                .build();
    }
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            // 저장하기 전에 TSID를 생성해서 넣음
            this.id = TsidCreator.getTsid().toLong();
        }
    }

}
