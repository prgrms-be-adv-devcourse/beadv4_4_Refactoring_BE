package com.thock.back.api.boundedContext.payment.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_revenue_logs")
public class RevenueLog extends BaseIdAndTime {
    @ManyToOne(fetch = LAZY)
    private PaymentMember member;

    @ManyToOne(fetch = LAZY)
    private Wallet wallet;

    private EventType eventType;

    private Long amount;

    private Long balance;

    public RevenueLog(PaymentMember member, Wallet wallet, EventType eventType, Long amount, Long balance) {
        this.member = member;
        this.wallet = wallet;
        this.eventType = eventType;
        this.amount = amount;
        this.balance = balance;
    }
}
