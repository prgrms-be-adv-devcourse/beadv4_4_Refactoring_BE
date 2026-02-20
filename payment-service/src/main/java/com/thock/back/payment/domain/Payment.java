package com.thock.back.payment.domain;


import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.jpa.entity.BaseIdAndTime;
import com.thock.back.payment.out.event.PaymentAddPaymentLogEvent;
import com.thock.back.shared.payment.dto.PaymentDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_payments")
//d
public class Payment extends BaseIdAndTime {
    private Long amount;

    private String orderId;

    private String paymentKey;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @ManyToOne(fetch = LAZY)
    private PaymentMember buyer;

    private Long pgAmount;

    private Long refundedAmount;

    @Version   // 낙관적 락 추가
    private Long version;

    public Payment(Long pgAmount, PaymentMember buyer, PaymentStatus status, String orderId, Long amount, String paymentKey) {
        this.pgAmount = pgAmount;
        this.buyer = buyer;
        this.status = status;
        this.orderId = orderId;
        this.amount = amount;
        this.refundedAmount = 0L;
        this.paymentKey = paymentKey;
    }

    public PaymentDto toDto(){
        return new PaymentDto(
                getId(),
                getOrderId(),
                getPaymentKey(),
                getBuyer().getId(),
                getPgAmount(),
                getAmount(),
                getCreatedAt(),
                getRefundedAmount()
        );
    }
    public void createPaymentLogEvent(){
        publishEvent(
                new PaymentAddPaymentLogEvent(
                        toDto()
                )
        );
    }

    public void updatePaymentStatus(PaymentStatus status){
        this.status = status;
    }

    public void updatePaymentKey(String paymentKey){
        this.paymentKey = paymentKey;
    }

    public boolean updatePaymentRefundedAmount(Long amount){
        if (this.refundedAmount+amount>this.amount){
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT);
        }
        this.refundedAmount += amount;
        return true;
    }
}
