package com.thock.back.shared.payment.event;

import com.thock.back.shared.payment.dto.RefundResponseDto;

public record PaymentRefundCompletedEvent(
        RefundResponseDto dto
) {}
