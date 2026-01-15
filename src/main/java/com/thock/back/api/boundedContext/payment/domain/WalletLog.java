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
@Table(name = "payment_wallet_logs")
public class WalletLog extends BaseIdAndTime {
    @ManyToOne(fetch = LAZY)
    private PaymentMember member;

    @ManyToOne(fetch = LAZY)
    private Wallet wallet;

    private EventType eventType;

    private Long amount;

    private Long balance;
}
