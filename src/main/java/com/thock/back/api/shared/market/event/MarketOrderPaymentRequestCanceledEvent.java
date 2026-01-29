package com.thock.back.api.shared.market.event;

import com.thock.back.api.shared.payment.dto.PaymentCancelRequestDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketOrderPaymentRequestCanceledEvent {
    private PaymentCancelRequestDto dto;
}
