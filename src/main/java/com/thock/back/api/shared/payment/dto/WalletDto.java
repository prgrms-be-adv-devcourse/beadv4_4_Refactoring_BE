package com.thock.back.api.shared.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class WalletDto {
    private final Long id;
    private final Long holderId;
    private final String holderName;
    private final Long balance;
    private final Long revenue;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
