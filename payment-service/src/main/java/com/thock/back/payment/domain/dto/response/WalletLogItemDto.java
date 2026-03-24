package com.thock.back.payment.domain.dto.response;

import com.thock.back.payment.domain.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class WalletLogItemDto {
    private Long id;
    private EventType eventType;
    private Long amount;
    private Long balance;
    private LocalDateTime createdAt;
}
