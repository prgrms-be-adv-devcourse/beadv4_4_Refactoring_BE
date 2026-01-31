package com.thock.back.shared.payment.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentCancelRequestDto {
    private String orderId;
    private String cancelReason;
    @Nullable
    // amount == null 이면 전액 환불
    private Long amount;
}
