package com.thock.back.settlement.reconciliation.domain;

import com.github.f4b6a3.tsid.TsidCreator;
import com.thock.back.settlement.reconciliation.domain.enums.MismatchType;
import com.thock.back.settlement.shared.money.Money;
import com.thock.back.settlement.shared.money.MoneyAttributeConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reconciliation_mismatch_log")
public class ReconciliationMismatchLog {

    @Id
    private Long id; // TSID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ReconciliationResult job;

    @Column(nullable = false)
    private String orderNo;

    private String pgKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MismatchType type;

    @Convert(converter = MoneyAttributeConverter.class)
    private Money internalAmount;
    @Convert(converter = MoneyAttributeConverter.class)
    private Money pgAmount;
    @Convert(converter = MoneyAttributeConverter.class)
    private Money diffAmount;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = TsidCreator.getTsid().toLong();
        }
    }

    @Builder
    public ReconciliationMismatchLog(ReconciliationResult job, String orderNo, String pgKey,
                                     MismatchType type, Money internalAmount, Money pgAmount, String reason) {
        this.job = job;
        this.orderNo = orderNo;
        this.pgKey = pgKey;
        this.type = type;
        this.internalAmount = internalAmount;
        this.pgAmount = pgAmount;
        this.reason = reason;

        Money safePg = pgAmount != null ? pgAmount : Money.zero();
        Money safeInternal = internalAmount != null ? internalAmount : Money.zero();
        this.diffAmount = safePg.minus(safeInternal);
    }
}
