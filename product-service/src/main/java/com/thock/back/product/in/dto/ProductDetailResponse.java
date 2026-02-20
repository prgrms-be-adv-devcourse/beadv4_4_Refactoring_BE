package com.thock.back.product.in.dto;

import com.thock.back.product.domain.entity.Product;

public record ProductDetailResponse (
        Long id,
        String name,
        Long price,
        String description,
        Integer stock,
        String category
) {
    public ProductDetailResponse(Product product) {
        this(
                product.getId(),
                product.getName(),
                product.getSalePrice(),
                product.getDescription(),
                product.getStock(),
                product.getCategory().name()
        );
    }
}