package com.thock.back.market.out.api.dto;

import lombok.Value;

@Value
public class ProductInfo {
    Long id;
    Long sellerId;
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
