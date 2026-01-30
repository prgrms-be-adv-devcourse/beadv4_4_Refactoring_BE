package com.thock.back.api.shared.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefundResponseDto {
    private Long memberId;
    private String orderId;
}
