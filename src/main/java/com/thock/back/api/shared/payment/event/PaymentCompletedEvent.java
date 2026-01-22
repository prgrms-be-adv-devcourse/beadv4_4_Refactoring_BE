package com.thock.back.api.shared.payment.event;

import com.thock.back.api.shared.payment.dto.PaymentDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentCompletedEvent {
    private final PaymentDto payment;
}
