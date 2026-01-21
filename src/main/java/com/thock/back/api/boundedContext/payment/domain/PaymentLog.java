package com.thock.back.api.boundedContext.payment.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_payment_logs")
public class PaymentLog extends BaseIdAndTime {
    @ManyToOne(fetch = LAZY)
    private PaymentMember buyer;

    private String orderId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private Long amount;

    private Long pgAmount;

    public PaymentLog(PaymentMember buyer, String orderId, PaymentStatus paymentStatus, Long amount, Long pgAmount) {
        this.buyer = buyer;
        this.orderId = orderId;
        this.paymentStatus = paymentStatus;
        this.amount = amount;
        this.pgAmount = pgAmount;
    }
}
