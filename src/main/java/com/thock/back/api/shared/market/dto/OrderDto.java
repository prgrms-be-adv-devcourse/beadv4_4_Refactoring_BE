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
    private final String state; // OrderState
    private final Long totalPrice;
    private final Long totalSalePrice;
    private final LocalDateTime requestPaymentDate;
    private final LocalDateTime paymentDate;
}
