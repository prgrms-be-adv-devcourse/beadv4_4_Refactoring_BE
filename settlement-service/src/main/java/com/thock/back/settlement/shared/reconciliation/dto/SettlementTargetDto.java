package com.thock.back.settlement.shared.reconciliation.dto;

import java.math.BigDecimal;

public record SettlementTargetDto (
        Long snapshotId,
        Long sellerId,
        BigDecimal amount){
}
