package com.thock.back.payment.domain;


import com.thock.back.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_revenue_logs")
public class RevenueLog extends BaseIdAndTime {
    @ManyToOne(fetch = LAZY)
    private PaymentMember holder;

    @ManyToOne(fetch = LAZY)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private Long amount;

    private Long balance;

    public RevenueLog(PaymentMember holder, Wallet wallet, EventType eventType, Long amount, Long balance) {
        this.holder = holder;
        this.wallet = wallet;
        this.eventType = eventType;
        this.amount = amount;
        this.balance = balance;
    }
}
