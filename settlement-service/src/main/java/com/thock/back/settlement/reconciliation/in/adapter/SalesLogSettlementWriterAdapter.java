package com.thock.back.settlement.reconciliation.in.adapter;

import com.thock.back.settlement.reconciliation.app.port.MarkSalesLogsSettledPort;
import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SalesLogSettlementWriterAdapter implements MarkSalesLogsSettledPort {

    private final SalesLogRepository salesLogRepository;

    @Override
    @Transactional
    public void markAsSettled(List<Long> salesLogIds, Long dailySettlementId) {
        List<SalesLog> targetLogs = salesLogRepository.findAllById(salesLogIds);
        targetLogs.forEach(log -> log.markAsSettled(dailySettlementId));
    }
}
