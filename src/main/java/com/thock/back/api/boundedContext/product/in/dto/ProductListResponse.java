package com.thock.back.api.boundedContext.product.in.dto;

import com.thock.back.api.boundedContext.product.domain.Product;
import lombok.Getter;

@Getter
public class ProductListResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private Long price;
    private String nickname;

    //현재는 판매자 + SellerId로 닉네임이 나오지만, 나중엔 member의 nickname을 가져오자.
    public ProductListResponse(Product product){
        this.id = product.getId();
        this.name = product.getName();
        this.imageUrl = product.getImageUrl();
        this.price = product.getPrice();
        this.nickname = "판매자" + " " + product.getSellerId();
    }
}
