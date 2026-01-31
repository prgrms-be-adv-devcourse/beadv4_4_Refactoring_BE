package com.thock.back.payment.out.event;


import com.thock.back.shared.payment.dto.PaymentDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentAddPaymentLogEvent {
    private final PaymentDto payment;
}
