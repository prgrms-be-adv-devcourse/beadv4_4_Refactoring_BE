package com.thock.back.payment.domain;


import com.thock.back.global.jpa.entity.BaseIdAndTime;
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

    @ManyToOne(fetch = LAZY)
    private Payment payment;

    public PaymentLog(PaymentMember buyer, String orderId, PaymentStatus paymentStatus, Long amount, Long pgAmount, Payment payment) {
        this.buyer = buyer;
        this.orderId = orderId;
        this.paymentStatus = paymentStatus;
        this.amount = amount;
        this.pgAmount = pgAmount;
        this.payment = payment;
    }
}
