package com.thock.back.api.boundedContext.settlement.in;

import com.thock.back.api.shared.settlement.dto.SettlementOrderDto;
import com.thock.back.api.shared.settlement.port.MarketDataPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

//@Component
//@Primary // <-- 중요! "진짜가 오기 전까진 내가 대장이다"라는 뜻
public class FakeMarketDataAdapter implements MarketDataPort {

    @Override
    public List<SettlementOrderDto> getSettlementTargetOrders(Long sellerId, LocalDate date) {
        // 실제 DB 조회 안 하고, 그냥 가짜 데이터 리턴!
        return List.of(
                SettlementOrderDto.builder()
                        .orderId(100L)
                        .orderItemId(1L)
                        .productId(55L)
                        .productName("테스트용 키보드")
                        .quantity(1)
                        .totalSalePrice(10000L) // 1만원짜리 팔림
                        .payoutAmount(9500L)    // 정산금 9500원
                        .feeAmount(500L)        // 수수료 500원
                        .confirmedAt(LocalDateTime.now().minusDays(1))
                        .build()
        );
    }
}