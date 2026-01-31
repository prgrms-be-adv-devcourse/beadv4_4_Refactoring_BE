package com.thock.back.shared.payment.event;

import com.thock.back.shared.payment.dto.PaymentDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentFailedEvent {
    private final PaymentDto payment;
}
