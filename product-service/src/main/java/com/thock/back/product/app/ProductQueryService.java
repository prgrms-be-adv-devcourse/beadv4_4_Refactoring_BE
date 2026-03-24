package com.thock.back.product.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.product.domain.Category;
import com.thock.back.product.domain.entity.Product;
import com.thock.back.product.in.dto.ProductDetailResponse;
import com.thock.back.product.in.dto.ProductListResponse;
import com.thock.back.product.in.dto.internal.ProductInternalResponse;
import com.thock.back.product.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {
    private final ProductRepository productRepository;

    public Page<ProductListResponse> searchProductsByCategory(Category category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable)
                .map(ProductListResponse::new);
    }

    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return new ProductDetailResponse(product);
    }

    public List<ProductListResponse> searchProductsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return productRepository.findByNameContaining(keyword).stream()
                .map(ProductListResponse::new)
                .toList();
    }

    public List<ProductInternalResponse> getProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return productRepository.findAllByIdIn(productIds).stream()
                .map(ProductInternalResponse::new)
                .toList();
    }

    public Page<ProductListResponse> getMyProducts(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable)
                .map(ProductListResponse::new);
    }
}
