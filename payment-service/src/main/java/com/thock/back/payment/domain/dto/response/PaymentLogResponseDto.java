package com.thock.back.payment.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PaymentLogResponseDto {
    private Long memberId;
    private List<PaymentLogItemDto> paymentLog;
}
