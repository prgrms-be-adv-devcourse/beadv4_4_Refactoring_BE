package com.thock.back.api.shared.market.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderDto {
    private final Long orderId;
    private final Long buyerId;
    private final Long price;
    // TODO: 주문 상태 ENUM 추가
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
