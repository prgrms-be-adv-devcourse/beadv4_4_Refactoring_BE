package com.thock.back.settlement.reconciliation.app.port;

import java.util.List;

public interface MarkSalesLogsSettledPort {
    void markAsSettled(List<Long> salesLogIds, Long dailySettlementId);
}
