package com.thock.back.shared.payment.event;

import com.thock.back.shared.payment.dto.PaymentDto;

public record PaymentFailedEvent(
        PaymentDto payment
) {}
