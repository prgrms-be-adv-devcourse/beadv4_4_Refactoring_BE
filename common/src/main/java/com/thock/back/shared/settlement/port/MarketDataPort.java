package com.thock.back.shared.settlement.port;
import com.thock.back.shared.settlement.dto.SettlementOrderDto;
import java.time.LocalDate;
import java.util.List;

public interface MarketDataPort {
    List<SettlementOrderDto> getSettlementTargetOrders(Long sellerId, LocalDate date);
}
