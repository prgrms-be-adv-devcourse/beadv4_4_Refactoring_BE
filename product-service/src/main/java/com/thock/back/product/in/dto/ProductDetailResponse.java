package com.thock.back.product.in.dto;

import com.thock.back.product.domain.Product;
import lombok.Getter;

@Getter
public class ProductDetailResponse {
    private final Long id;
    private final String name;
    private final Long price;
    private final String description; // 상세 설명 추가
    private final Integer stock;      // 재고 추가
    private final String category;    // 카테고리 추가
    // private final String sellerNickname; // 필요하면 추가

    public ProductDetailResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.price = product.getSalePrice();
        this.description = product.getDescription();
        this.stock = product.getStock();
        this.category = product.getCategory().name();
    }
}