package com.thock.back.api.shared.payment.dto;

import com.thock.back.api.boundedContext.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefundResponseDto {
    private Long memberId;
    private String orderId;
    private PaymentStatus status;
}
