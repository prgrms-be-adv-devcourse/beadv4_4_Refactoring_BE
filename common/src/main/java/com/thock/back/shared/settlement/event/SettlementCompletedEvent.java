package com.thock.back.shared.settlement.event;

public record SettlementCompletedEvent(
        Long memberID,
        Long amount
) {}
