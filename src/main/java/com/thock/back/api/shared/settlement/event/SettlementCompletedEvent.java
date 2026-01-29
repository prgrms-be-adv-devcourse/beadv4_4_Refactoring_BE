package com.thock.back.api.shared.settlement.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class SettlementCompletedEvent {
    private Long memberID;
    private Long amount;
}
