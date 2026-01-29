package com.thock.back.api.boundedContext.product.in.dto.internal;

import com.thock.back.api.boundedContext.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


// 장바구니에 담은 상품들에 대한 상세정보
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductInternalResponse {
    private Long id;
    private Long sellerId;
    private String name;
    private String imageUrl; // 마켓이 원함
    private Long price;
    private Long salePrice;  // 마켓이 원함
    private Integer stock;
    private String state;    // ENUM 대신 String으로 줘서 마켓이 알아서 파싱하게 함

    public ProductInternalResponse(Product product) {
        this.id = product.getId();
        this.sellerId = product.getSellerId();
        this.name = product.getName();
        this.imageUrl = product.getImageUrl();
        this.price = product.getPrice();
        this.salePrice = product.getSalePrice();
        this.stock = product.getStock();
        this.state = product.getState().name(); // "ON_SALE" 문자열로 변환
    }
}