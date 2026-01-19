package com.thock.back.api.shared.payment.event;

import com.thock.back.api.boundedContext.payment.domain.PaymentStatus;
import com.thock.back.api.shared.payment.dto.PaymentDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentAddPaymentLogEvent {
    private final PaymentDto payment;
}
