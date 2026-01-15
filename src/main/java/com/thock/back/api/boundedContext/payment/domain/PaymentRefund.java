package com.thock.back.api.boundedContext.payment.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_payment_refunds")
public class PaymentRefund extends BaseIdAndTime {
    @OneToOne
    private Payment payment;

    private Long amount;

    private PaymentStatus status;
}
