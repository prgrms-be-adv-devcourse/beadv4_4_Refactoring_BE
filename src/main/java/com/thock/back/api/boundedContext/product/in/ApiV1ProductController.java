package com.thock.back.api.boundedContext.product.in;

import com.thock.back.api.boundedContext.product.app.ProductService;
import com.thock.back.api.boundedContext.product.domain.Category;
import com.thock.back.api.boundedContext.product.in.dto.ProductDetailResponse;
import com.thock.back.api.boundedContext.product.in.dto.ProductListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ApiV1ProductController {

    private final ProductService productService;

    // 1. 카테고리별 상품 리스트 조회
    // 요청: GET /api/v1/products?category=KEYBOARD
    @GetMapping
    public List<ProductListResponse> list(@RequestParam Category category) {
        return productService.searchByCategory(category);
    }

    // 2. 상품 상세 정보 조회
    // 요청: GET /api/v1/products/1
    @GetMapping("/{id}")
    public ProductDetailResponse detail(@PathVariable Long id) {
        return productService.productDetail(id);
    }
}