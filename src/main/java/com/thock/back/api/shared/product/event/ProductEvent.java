package com.thock.back.api.shared.product.event;


import lombok.Builder;

@Builder
public class ProductEvent {
    private Long productId;
    private Long sellerId;
    private String name;
    private Long price;
    private Long salePrice;
    private String description;
    private Integer stock;
    private String imageUrl;
    private String productState;
    private ProductEventType eventType; // "CREATE", "UPDATE", "DELETE"
}
