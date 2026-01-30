package com.thock.back.shared.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class PaymentDto {
    private Long id;
    private String orderId;
    private String paymentKey;
    private Long buyerId;
    private Long pgAmount;
    private Long amount;
    private LocalDateTime createdAt;
}