package com.thock.back.shared.market.event;

import com.thock.back.shared.payment.dto.BeforePaymentCancelRequestDto;

public record MarketOrderBeforePaymentCanceledEvent(
        BeforePaymentCancelRequestDto dto
) {
}
