package com.thock.back.api.boundedContext.payment.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import com.thock.back.api.shared.payment.dto.PaymentDto;
import com.thock.back.api.shared.payment.event.PaymentAddPaymentLogEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_payments")
public class Payment extends BaseIdAndTime {
    private Long amount;

    private String orderId;

    private String paymentKey;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @ManyToOne(fetch = LAZY)
    private PaymentMember buyer;

    @ManyToOne(fetch = LAZY)
    private Wallet wallet;

    private Long pgAmount;

    @OneToMany
    private static List<PaymentLog> paymentLogs = new ArrayList<>();

    public Payment(Long pgAmount, PaymentMember buyer, PaymentStatus status, String orderId, Long amount, String paymentKey) {
        this.pgAmount = pgAmount;
        this.buyer = buyer;
        this.status = status;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentKey = paymentKey;

        publishEvent(
                new PaymentAddPaymentLogEvent(
                        toDto()
                )
        );
    }

    public PaymentDto toDto(){
        return new PaymentDto(
                getId(),
                getOrderId(),
                getPaymentKey(),
                getBuyer().getId(),
                getStatus(),
                getPgAmount(),
                getAmount(),
                getCreatedAt()
        );
    }

    public void updatePaymentStatus(PaymentStatus status){
        this.status = status;
        publishEvent(
                new PaymentAddPaymentLogEvent(
                        toDto()
                )
        );
    }
}
