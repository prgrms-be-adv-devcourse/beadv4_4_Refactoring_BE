package com.thock.back.api.boundedContext.market.out.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// TODO : 필드 수정
public class ProductInfo {
    private Long id;
    private String name;
    private String imageUrl;
    private Long price;
    private Long salePrice;
    private Integer stock;
//    private boolean isAvailable; 품절인지 아닌지

    /**
     * 👇 ProductState를 그대로 받아오면 안됨.
     * product 모듈을 import 하게 되기 때문.
     */
    // private ProductState state


}
