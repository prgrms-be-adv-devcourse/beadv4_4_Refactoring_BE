package com.thock.back.shared.payment.dto;

import java.time.LocalDateTime;

public record PaymentDto(
        Long id,
        String orderId,
        String paymentKey,
        Long buyerId,
        Long pgAmount,
        Long amount,
        LocalDateTime createdAt,
        Long refundedAmount
) {}
