package com.thock.back.shared.payment.dto;

import java.time.LocalDateTime;

public record WalletDto(
        Long id,
        Long holderId,
        String holderName,
        Long balance,
        Long revenue,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
