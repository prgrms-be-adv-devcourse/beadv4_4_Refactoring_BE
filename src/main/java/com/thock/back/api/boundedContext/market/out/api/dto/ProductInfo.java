package com.thock.back.api.boundedContext.market.out.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@Value
public class ProductInfo {
    Long id;
    String name;
    String imageUrl;
    Long price;
    Long salePrice;
    Integer stock;
    String state; // ProductState

    public boolean isAvailable() {
        return "ON_SALE".equals(state) && stock != null && stock > 0;
    }
}
