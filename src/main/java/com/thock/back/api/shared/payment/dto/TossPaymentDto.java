package com.thock.back.api.shared.payment.dto;

import com.thock.back.api.shared.market.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TossPaymentDto {
    private PaymentDto paymentDto;
    private String successUrl;
    private String failUrl;
}
