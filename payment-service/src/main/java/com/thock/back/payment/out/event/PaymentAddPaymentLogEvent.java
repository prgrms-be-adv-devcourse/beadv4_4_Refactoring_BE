package com.thock.back.payment.out.event;


import com.thock.back.shared.payment.dto.PaymentDto;

public record PaymentAddPaymentLogEvent(
        PaymentDto payment
) {}
