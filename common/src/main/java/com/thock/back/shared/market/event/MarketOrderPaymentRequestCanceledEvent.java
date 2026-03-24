package com.thock.back.shared.market.event;

import com.thock.back.shared.payment.dto.PaymentCancelRequestDto;

public record MarketOrderPaymentRequestCanceledEvent(
        PaymentCancelRequestDto dto
) {}
