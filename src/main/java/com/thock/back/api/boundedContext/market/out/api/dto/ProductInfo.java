package com.thock.back.api.boundedContext.market.out.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductInfo {
    private Long id;
    private String name;
    private String imageUrl;
    private Long price;
    private Long salePrice;
    private Integer stock;
    private String state; // ProductState
}
