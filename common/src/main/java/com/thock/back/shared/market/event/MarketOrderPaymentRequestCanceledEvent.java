package com.thock.back.shared.market.event;

import com.thock.back.shared.payment.dto.PaymentCancelRequestDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketOrderPaymentRequestCanceledEvent {
    private PaymentCancelRequestDto dto;
}
