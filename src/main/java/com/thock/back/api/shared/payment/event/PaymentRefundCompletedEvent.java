package com.thock.back.api.shared.payment.event;

import com.thock.back.api.shared.payment.dto.RefundResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentRefundCompletedEvent {
    private RefundResponseDto dto;
}
