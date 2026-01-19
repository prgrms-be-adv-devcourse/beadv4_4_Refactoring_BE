package com.thock.back.api.shared.market.event;

import com.thock.back.api.shared.market.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MarketOrderPaymentCanceledEvent {
    // TODO: 만약 주문이 취소되면 payment에 이벤트 발행 해야함
    private OrderDto order;
}
