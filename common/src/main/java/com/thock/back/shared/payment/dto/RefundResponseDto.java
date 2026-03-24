package com.thock.back.shared.payment.dto;

public record RefundResponseDto(
        Long memberId,
        String orderId,
        Long amount
) {}
