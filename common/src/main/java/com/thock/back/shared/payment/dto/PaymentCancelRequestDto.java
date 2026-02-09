package com.thock.back.shared.payment.dto;

public record PaymentCancelRequestDto(
        String orderId,
        String cancelReason,
        Long amount // amount == totalSalePrice 이면 전액 환불 ? 부분 환불
) {}