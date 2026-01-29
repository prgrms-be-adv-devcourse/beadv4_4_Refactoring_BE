package com.thock.back.api.boundedContext.payment.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PaymentConfirmRequestDto {
    private String paymentKey;
    private String orderId;
    private Long amount;
}