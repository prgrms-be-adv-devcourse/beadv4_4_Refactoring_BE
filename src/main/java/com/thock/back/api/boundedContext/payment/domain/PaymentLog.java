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
    private PaymentMember member;

    @ManyToOne(fetch = LAZY)
    private Payment payment;

    @ManyToOne(fetch = LAZY)
    private PaymentRefund refund;

    @Enumerated(EnumType.STRING)
    private EventType eventType;
}
