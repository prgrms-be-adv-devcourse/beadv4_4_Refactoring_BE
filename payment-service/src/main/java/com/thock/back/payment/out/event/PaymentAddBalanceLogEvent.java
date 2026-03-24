package com.thock.back.payment.out.event;


import com.thock.back.payment.domain.EventType;
import com.thock.back.shared.payment.dto.WalletDto;

public record PaymentAddBalanceLogEvent(
        WalletDto wallet,
        EventType eventType,
        Long amount
) {}
