package com.thock.back.payment.domain;


import com.thock.back.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_wallet_logs")
public class WalletLog extends BaseIdAndTime {
    @ManyToOne(fetch = LAZY)
    private PaymentMember holder;

    @ManyToOne(fetch = LAZY)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private Long amount;

    private Long balance;

    public WalletLog(PaymentMember holder, Wallet wallet, EventType eventType, Long amount, Long balance) {
        this.holder = holder;
        this.wallet = wallet;
        this.eventType = eventType;
        this.amount = amount;
        this.balance = balance;
    }
}
