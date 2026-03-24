package com.thock.back.payment.domain.dto.response;

public record TossErrorResponseDto(
        String code,
        String message
) {}