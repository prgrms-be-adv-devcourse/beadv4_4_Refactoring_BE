package com.thock.back.shared.market.dto;

public record OrderDto(
        Long id,
        Long buyerId,
        String buyerName,
        String orderNumber,
        Long totalSalePrice
) {}
