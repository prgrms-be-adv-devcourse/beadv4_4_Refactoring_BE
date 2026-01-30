package com.thock.back.api.shared.market.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderDto {
    private final Long id;
    private final Long buyerId;
    private final String buyerName;
    private final String orderNumber;
    private final Long totalSalePrice; // 실제로 전체 지불해야 할 금액
}
