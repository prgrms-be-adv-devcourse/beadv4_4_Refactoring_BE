package com.thock.back.payment.domain.dto.response;

import com.thock.back.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentLogItemDto {
    private Long id;
    private String orderId;
    private PaymentStatus paymentStatus;
    private Long amount;
    private Long pgAmount;
    private Long refundedAmount;
    private LocalDateTime createdAt;
}
