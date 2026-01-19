package com.thock.back.api.boundedContext.payment.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import com.thock.back.api.shared.payment.dto.PaymentDto;
import com.thock.back.api.shared.payment.event.PaymentAddBalanceLogEvent;
import com.thock.back.api.shared.payment.event.PaymentAddPaymentLogEvent;
import com.thock.back.api.shared.payment.event.PaymentAddRevenueLogEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_payments")
public class Payment extends BaseIdAndTime {
    private Long amount;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @ManyToOne(fetch = LAZY)
    private PaymentMember buyer;

    @ManyToOne(fetch = LAZY)
    private Wallet wallet;

    private Long pgAmount;

    @OneToMany
    private static List<PaymentLog> paymentLogs = new ArrayList<>();

    public void createRequested(Long amount, Long orderId, PaymentStatus status, PaymentMember buyer, Long pgAmount) {
        this.amount = amount;
        this.orderId = orderId;
        this.status = status;
        this.buyer = buyer;
        this.pgAmount = pgAmount;

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
                getBuyer().getId(),
                getWallet().getId(),
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
