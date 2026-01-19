package com.thock.back.api.shared.payment.dto;

import com.thock.back.api.boundedContext.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class PaymentDto {
    private Long id;
    private Long orderId;
    private Long buyerId;
    private Long walletId;
    private PaymentStatus status;
    private Long pgAmount;
    private Long amount;
    private LocalDateTime createdAt;
}
