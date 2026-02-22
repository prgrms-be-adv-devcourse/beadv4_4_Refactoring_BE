package com.thock.back.product.in.dto.internal;

import com.thock.back.product.domain.entity.Product;

// 장바구니에 담은 상품들에 대한 상세정보
public record ProductInternalResponse (
        Long id,
        Long sellerId,
        String name,
        String imageUrl,
        Long price,
        Long salePrice,
        Integer stock,
        String state
) {
    public ProductInternalResponse(Product product) {
        this(
                product.getId(),
                product.getSellerId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                product.getSalePrice(),
                product.getStock(),
                product.getState().name()
        );
    }
}