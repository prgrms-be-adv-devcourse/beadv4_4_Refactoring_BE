package com.thock.back.api.shared.payment.event;

import com.thock.back.api.boundedContext.payment.domain.EventType;
import com.thock.back.api.shared.payment.dto.WalletDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentAddBalanceLogEvent {
    private final WalletDto wallet;
    private final EventType eventType;
    private final Long amount;
}
