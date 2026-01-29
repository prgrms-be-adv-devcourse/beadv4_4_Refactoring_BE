package com.thock.back.api.boundedContext.payment.domain.dto.response;

import com.thock.back.api.boundedContext.payment.domain.PaymentLog;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PaymentLogResponseDto {
    private Long memberId;
    private List<PaymentLog> paymentLog;
}
