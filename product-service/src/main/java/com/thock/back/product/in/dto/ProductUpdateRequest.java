package com.thock.back.product.in.dto;

import com.thock.back.product.domain.Category;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class ProductUpdateRequest {
    private String name;
    private Long price;
    private Long salePrice;
    private Integer stock;
    private Category category;
    private String description;
    private String imageUrl;
    private Map<String, Object> detail;
}
