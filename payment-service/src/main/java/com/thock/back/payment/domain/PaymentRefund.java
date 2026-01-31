package com.thock.back.payment.domain;


import com.thock.back.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.*;
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

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
}
