package com.thock.back.product.in.dto;

import com.thock.back.product.domain.Category;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class ProductCreateRequest {
    private String name;            // 상품명
    private Long price;             // 정가
    private Long salePrice;         // 할인가
    private Integer stock;          // 재고
    private Category category;      // 카테고리 (ENUM)
    private String description;     // 간단 설명 (TEXT)
    private String imageUrl;        // 대표 이미지 URL

    private Map<String, Object> detail;

}
