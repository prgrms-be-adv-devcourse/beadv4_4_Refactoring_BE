package com.thock.back.shared.payment.event;

import com.thock.back.shared.payment.dto.RefundResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentRefundCompletedEvent {
    private RefundResponseDto dto;
}
